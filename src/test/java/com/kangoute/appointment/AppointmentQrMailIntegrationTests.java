package com.kangoute.appointment;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.*;
import com.kangoute.appointment.security.JwtService;
import com.kangoute.appointment.service.*;
import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:qr-mail;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "app.mail.enabled=true", "app.mail.from=appointments@example.com", "app.frontend.url=https://portfolio.example.com"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppointmentQrMailIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired UserService users;
    @Autowired RoleService roles;
    @Autowired UserRepository userRepository;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentAuditRepository audits;
    @Autowired AppointmentNotificationRepository notifications;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockitoBean JavaMailSender sender;
    @MockitoBean Clock clock;
    private MockMvc mvc;
    private User owner;
    private User admin;
    private MimeMessage sent;

    @BeforeEach
    void setUp() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2030-01-07T08:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        notifications.deleteAll();
        audits.deleteAll();
        appointments.deleteAll();
        userRepository.deleteAll();
        owner = users.createUser(User.builder().firstName("Original").lastName("Profile")
                .email("owner@example.com").password("TestOnly2026!").build());
        admin = users.createUser(User.builder().firstName("Real").lastName("Admin")
                .email("admin@example.com").password("TestOnly2026!").build());
        admin.setRoles(Set.of(roles.createRole(RoleName.ROLE_ADMIN)));
        admin = userRepository.saveAndFlush(admin);
        sent = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(sent);
        doAnswer(invocation -> {
            invocation.getArgument(0, MimeMessage.class).saveChanges();
            return null;
        }).when(sender).send(any(MimeMessage.class));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void committedBookingSendsPendingEmailToContactWithReferenceAndScannableQr() throws Exception {
        book(payload()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.contactFirstName").value("Jean"))
                .andExpect(jsonPath("$.contactLastName").value("Dupont"))
                .andExpect(jsonPath("$.contactEmail").value("contact@example.com"))
                .andExpect(jsonPath("$.publicReference").isNotEmpty())
                .andExpect(jsonPath("$.verificationToken").doesNotExist());
        var appointment = appointments.findAll().getFirst();
        assertEquals("Original", userRepository.findById(owner.getId()).orElseThrow().getFirstName());
        verify(sender).send(any(MimeMessage.class));
        assertEquals("contact@example.com", sent.getAllRecipients()[0].toString());
        assertTrue(sent.getSubject().startsWith("Demande de rendez-vous enregistrée"));
        String html = html(sent);
        assertTrue(html.contains("En attente de confirmation"));
        assertFalse(html.contains("Votre rendez-vous est confirmé"));
        assertTrue(html.contains(appointment.getPublicReference()));
        assertTrue(html.contains("cid:appointmentQr"));
        byte[] png = png(sent);
        assertNotNull(png);
        assertTrue(png.length > 0);
        String decoded = decode(png);
        assertEquals("https://portfolio.example.com/verify-appointment?token=" + appointment.getVerificationToken(), decoded);
        assertFalse(decoded.contains("Jean"));
        assertFalse(decoded.contains("contact@example.com"));
        assertTrue(appointment.getVerificationToken().matches("[A-Za-z0-9_-]{43}"));
        assertTrue(appointments.findByPublicReference(appointment.getPublicReference()).isPresent());
    }

    @Test
    void publicVerificationReturnsOnlySafeFieldsAndOtherEndpointsStayProtected() throws Exception {
        book(payload()).andExpect(status().isCreated());
        var appointment = appointments.findAll().getFirst();
        mvc.perform(get("/api/public/appointments/verify").param("token", appointment.getVerificationToken()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.publicReference").value(appointment.getPublicReference()))
                .andExpect(jsonPath("$.contactFirstName").value("Jean"))
                .andExpect(jsonPath("$.contactLastName").value("Dupont"))
                .andExpect(jsonPath("$.reason").value("Consultation"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.contactEmail").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.verificationToken").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/public/appointments/verify").param("token", "invalid"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/public/appointments/verify").param("token", "A".repeat(43)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/appointments")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"contactFirstName,' '", "contactLastName,' '", "contactEmail,' '", "contactEmail,invalid",
            "contactFirstName,J", "contactLastName,D"})
    void invalidContactRejectsBookingWithoutEmail(String field, String value) throws Exception {
        Map<String, String> body = payload();
        body.put(field, value);
        book(body).andExpect(status().isBadRequest());
        assertEquals(0, appointments.count());
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void mailFailureDoesNotUndoCommittedAppointmentNotificationOrAudit() throws Exception {
        doThrow(new MailSendException("Simulated SMTP failure")).when(sender).send(any(MimeMessage.class));
        book(payload()).andExpect(status().isCreated());
        assertEquals(1, appointments.count());
        assertEquals(1, audits.count());
        assertEquals(1, notifications.findByRecipientIdOrderByCreatedAtDesc(owner.getId()).size());
    }

    @Test
    void adminConfirmationAndCancellationSendCorrectEmailsAndKeepSameVerificationLink() throws Exception {
        book(payload()).andExpect(status().isCreated());
        var appointment = appointments.findAll().getFirst();
        for (String state : new String[]{"CONFIRMED", "CANCELLED"}) {
            sent = new MimeMessage(Session.getInstance(new Properties()));
            when(sender.createMimeMessage()).thenReturn(sent);
            mvc.perform(patch("/api/admin/appointments/{id}/status", appointment.getId())
                            .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + state + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
            assertTrue(sent.getSubject().startsWith(state.equals("CONFIRMED") ? "Rendez-vous confirmé" : "Rendez-vous annulé"));
            assertTrue(html(sent).contains(appointment.getVerificationToken()));
            mvc.perform(get("/api/public/appointments/verify").param("token", appointment.getVerificationToken()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
        }
        verify(sender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void adminSeesContactDetailsCanSearchEachFieldAndReceivesNotification() throws Exception {
        book(payload()).andExpect(status().isCreated());
        var appointment = appointments.findAll().getFirst();
        for (String query : new String[]{appointment.getPublicReference(), "Jean", "Dupont", "contact@example.com"}) {
            mvc.perform(get("/api/admin/appointments").param("query", query).header("Authorization", bearer(admin)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].contactFirstName").value("Jean"))
                    .andExpect(jsonPath("$.content[0].contactLastName").value("Dupont"))
                    .andExpect(jsonPath("$.content[0].contactEmail").value("contact@example.com"))
                    .andExpect(jsonPath("$.content[0].verificationToken").doesNotExist());
        }
        var notification = notifications.findByRecipientIdOrderByCreatedAtDesc(admin.getId()).getFirst();
        assertEquals("Nouveau rendez-vous", notification.getTitle());
        for (String detail : new String[]{appointment.getPublicReference(), "Jean Dupont", "contact@example.com", "08/01/2030", "Consultation"}) {
            assertTrue(notification.getMessage().contains(detail));
        }
    }

    @Test
    void rejectedSecondActiveBookingDoesNotSendAnotherEmail() throws Exception {
        book(payload()).andExpect(status().isCreated());
        Map<String, String> next = payload();
        next.put("startDateTime", "2030-01-09T10:00:00");
        next.put("endDateTime", "2030-01-09T10:30:00");
        book(next).andExpect(status().isConflict());
        verify(sender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void rolledBackBookingNeverSendsEmail() {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            try {
                book(payload()).andExpect(status().isCreated());
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
            transaction.setRollbackOnly();
        });
        assertEquals(0, appointments.count());
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void excessivelyLongContactNamesAreRejected() throws Exception {
        for (String field : new String[]{"contactFirstName", "contactLastName"}) {
            Map<String, String> body = payload();
            body.put(field, "A".repeat(81));
            book(body).andExpect(status().isBadRequest());
        }
    }

    private Map<String, String> payload() {
        return new HashMap<>(Map.of("startDateTime", "2030-01-08T10:00:00", "endDateTime", "2030-01-08T10:30:00",
                "reason", "Consultation", "contactFirstName", " Jean ", "contactLastName", " Dupont ",
                "contactEmail", " CONTACT@EXAMPLE.COM "));
    }
    private ResultActions book(Map<String, String> body) throws Exception {
        return mvc.perform(post("/api/appointments").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    private String bearer(User user) { return "Bearer " + jwt.generateToken(user); }
    private String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = html(multipart.getBodyPart(i));
                if (!found.isEmpty()) return found;
            }
        }
        return "";
    }
    private byte[] png(Part part) throws Exception {
        if (part.isMimeType("image/png")) return part.getInputStream().readAllBytes();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                byte[] found = png(multipart.getBodyPart(i));
                if (found != null) return found;
            }
        }
        return null;
    }
    private String decode(byte[] png) throws Exception {
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))))).getText();
    }
}
