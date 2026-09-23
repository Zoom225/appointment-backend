package com.kangoute.appointment;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.AppointmentNotificationRepository;
import com.kangoute.appointment.security.JwtService;
import com.kangoute.appointment.service.UserService;
import com.kangoute.appointment.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AppointmentBookingIntegrationTests {
    private static final LocalDateTime START = LocalDateTime.of(2030, 1, 8, 10, 0);
    @Autowired WebApplicationContext context;
    @Autowired UserService users;
    @Autowired RoleService roles;
    @Autowired JwtService jwt;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentNotificationRepository notifications;
    @MockitoBean Clock clock;
    private MockMvc mvc;
    private User owner;
    private User other;
    private User admin;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2030-01-07T08:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = user("booking.owner@example.com");
        other = user("booking.other@example.com");
        admin = user("booking.admin@example.com");
        admin.setRoles(Set.of(roles.createRole(RoleName.ROLE_ADMIN)));
    }

    @Test
    void bookingUsesJwtIdentityAndCreatesPendingNotificationAndTimestamps() throws Exception {
        book(owner, START).andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(owner.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startDateTime").value("2030-01-08T10:00:00"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
        var messages = notifications.findByRecipientIdOrderByCreatedAtDesc(owner.getId());
        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().getMessage().contains("08/01/2030 à 10:00"));
        assertTrue(messages.getFirst().getMessage().contains("en attente"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2030-01-07T07:59:59", "2030-01-07T08:00:00"})
    void pastOrCurrentTimeIsRejected(String start) throws Exception {
        book(owner, LocalDateTime.parse(start)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La date du rendez-vous doit être dans le futur."))
                .andExpect(jsonPath("$.status").value(400));
        assertEquals(0, appointments.count());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PENDING", "SCHEDULED", "CONFIRMED"})
    void secondFutureActiveBookingIsRejected(AppointmentStatus state) throws Exception {
        seed(owner, START, state);
        book(owner, START.plusDays(1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vous avez déjà un rendez-vous actif. Annulez-le ou attendez sa finalisation avant d'en réserver un nouveau."));
        assertEquals(1, appointments.count());
        assertEquals(0, notifications.count());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "COMPLETED"})
    void terminalAppointmentAllowsAnotherBooking(AppointmentStatus state) throws Exception {
        seed(owner, START, state);
        book(owner, START).andExpect(status().isCreated());
    }

    @Test
    void cancellingCreatesNotificationAndReleasesSlot() throws Exception {
        var appointment = seed(owner, START, AppointmentStatus.CONFIRMED);
        mvc.perform(patch("/api/appointments/{id}/cancel", appointment.getId()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals("Votre rendez-vous a été annulé.", notifications.findByRecipientIdOrderByCreatedAtDesc(owner.getId()).getFirst().getMessage());
        book(owner, START).andExpect(status().isCreated());
    }

    @Test
    void anotherUsersSlotCannotBeBooked() throws Exception {
        seed(other, START, AppointmentStatus.PENDING);
        book(owner, START).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ce créneau n'est plus disponible."));
    }

    @Test
    void overlappingLongerSlotIsRejectedButAdjacentSlotIsAvailable() throws Exception {
        seed(other, START.plusMinutes(30), AppointmentStatus.CONFIRMED);
        mvc.perform(post("/api/appointments").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(payload(START, START.plusMinutes(60))))
                .andExpect(status().isConflict());
        book(owner, START).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2030-01-08T08:30:00", "2030-01-08T10:10:00", "2030-01-08T10:00:01", "2030-01-12T10:00:00"})
    void invalidWorkingWindowIsRejected(String start) throws Exception {
        book(owner, LocalDateTime.parse(start)).andExpect(status().isBadRequest());
    }

    @Test
    void durationMustRespectSlotGrid() throws Exception {
        mvc.perform(post("/api/appointments").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(payload(START, START.plusMinutes(15))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotReadUpdateOrCancelAnotherUsersAppointment() throws Exception {
        Long id = seed(other, START, AppointmentStatus.PENDING).getId();
        mvc.perform(get("/api/appointments/{id}", id).header("Authorization", bearer(owner))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/appointments/{id}/cancel", id).header("Authorization", bearer(owner))).andExpect(status().isForbidden());
        mvc.perform(put("/api/appointments/{id}", id).header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(payload(START.plusHours(1), START.plusHours(2))))
                .andExpect(status().isForbidden());
    }

    @Test
    void suppliedUserIdCannotImpersonateAnotherUser() throws Exception {
        String body = payload(START, START.plusMinutes(30)).replace("}", ",\"userId\":" + other.getId() + "}");
        mvc.perform(post("/api/appointments").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRMED", "COMPLETED", "SCHEDULED"})
    void userCannotPerformAdminStatusActions(String status) throws Exception {
        Long id = seed(owner, START, AppointmentStatus.PENDING).getId();
        changeStatus(owner, "/api/appointments/" + id, status).andExpect(status().isForbidden());
        changeStatus(owner, "/api/admin/appointments/" + id + "/status", status).andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminListsOrStatistics() throws Exception {
        mvc.perform(get("/api/admin/appointments").header("Authorization", bearer(owner))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/statistics").header("Authorization", bearer(owner))).andExpect(status().isForbidden());
    }

    @Test
    void adminConfirmsAndCompletesWithAuditAndConfirmationNotification() throws Exception {
        Long id = seed(owner, START, AppointmentStatus.PENDING).getId();
        changeStatus(admin, "/api/admin/appointments/" + id + "/status", "CONFIRMED").andExpect(status().isOk());
        var confirmation = notifications.findByRecipientIdOrderByCreatedAtDesc(owner.getId()).getFirst();
        assertEquals("Rendez-vous confirmé", confirmation.getTitle());
        assertEquals("Votre rendez-vous du 08/01/2030 à 10:00 est confirmé.", confirmation.getMessage());
        changeStatus(admin, "/api/admin/appointments/" + id + "/status", "COMPLETED").andExpect(status().isOk());
        mvc.perform(get("/api/admin/appointments/{id}/history", id).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        book(owner, START.plusDays(1)).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "COMPLETED"})
    void terminalStatusCannotBeReopenedCancelledOrEdited(AppointmentStatus state) throws Exception {
        Long id = seed(owner, START, state).getId();
        changeStatus(admin, "/api/admin/appointments/" + id + "/status", "CONFIRMED").andExpect(status().isConflict());
        changeStatus(admin, "/api/admin/appointments/" + id + "/status", "COMPLETED").andExpect(status().isConflict());
        mvc.perform(patch("/api/appointments/{id}/cancel", id).header("Authorization", bearer(owner))).andExpect(status().isConflict());
        mvc.perform(put("/api/appointments/{id}", id).header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(payload(START.plusHours(1), START.plusHours(2))))
                .andExpect(status().isConflict());
    }

    @Test
    void pendingCannotSkipConfirmation() throws Exception {
        Long id = seed(owner, START, AppointmentStatus.PENDING).getId();
        changeStatus(admin, "/api/admin/appointments/" + id + "/status", "COMPLETED")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void reschedulingCannotStealOccupiedSlot() throws Exception {
        Long id = seed(owner, START, AppointmentStatus.PENDING).getId();
        seed(other, START.plusHours(1), AppointmentStatus.CONFIRMED);
        mvc.perform(put("/api/appointments/{id}", id).header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(payload(START.plusHours(1), START.plusHours(2))))
                .andExpect(status().isConflict());
    }

    @Test
    void upcomingAndHistoryAreScopedAndPaginated() throws Exception {
        seed(owner, START, AppointmentStatus.PENDING);
        seed(owner, START.plusDays(1), AppointmentStatus.CANCELLED);
        seed(owner, START.minusDays(3), AppointmentStatus.COMPLETED);
        seed(other, START.plusHours(1), AppointmentStatus.PENDING);
        mvc.perform(get("/api/appointments/me/upcoming").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(owner.getId()));
        mvc.perform(get("/api/appointments/me/history?size=1&page=1").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test
    void adminCanFilterByUserAndSeeDashboardCounts() throws Exception {
        seed(owner, START, AppointmentStatus.CONFIRMED);
        seed(other, START.minusDays(1), AppointmentStatus.COMPLETED);
        seed(owner, START.plusDays(1), AppointmentStatus.CANCELLED);
        mvc.perform(get("/api/admin/appointments?query=booking.owner&size=1").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/admin/statistics").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAppointments").value(3))
                .andExpect(jsonPath("$.todayAppointments").value(1))
                .andExpect(jsonPath("$.upcomingAppointments").value(1))
                .andExpect(jsonPath("$.completedAppointments").value(1))
                .andExpect(jsonPath("$.cancelledAppointments").value(1))
                .andExpect(jsonPath("$.totalUsers").value(3));
    }

    @Test
    void availabilityIsGlobalAndDoesNotShowPastSlots() throws Exception {
        seed(other, START, AppointmentStatus.CONFIRMED);
        mvc.perform(get("/api/appointments/availability?date=2030-01-08").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(17));
        mvc.perform(get("/api/appointments/availability?date=2030-01-04").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void openApiDocumentsPersonalViewsAndAdminStatistics() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/appointments/me/upcoming'].get").exists())
                .andExpect(jsonPath("$.paths['/api/appointments/me/history'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/statistics'].get").exists());
    }

    private User user(String email) {
        return users.createUser(User.builder().firstName("Booking").lastName("User").email(email).password("test-password").build());
    }
    private Appointment seed(User user, LocalDateTime start, AppointmentStatus state) {
        return appointments.saveAndFlush(Appointment.builder().user(user).startDateTime(start)
                .endDateTime(start.plusMinutes(30)).status(state).reason("Fixture").build());
    }
    private String bearer(User user) { return "Bearer " + jwt.generateToken(user); }
    private ResultActions book(User user, LocalDateTime start) throws Exception {
        return mvc.perform(post("/api/appointments").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(payload(start, start.plusMinutes(30))));
    }
    private ResultActions changeStatus(User user, String path, String state) throws Exception {
        return mvc.perform(patch(path).header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + state + "\"}"));
    }
    private String payload(LocalDateTime start, LocalDateTime end) {
        return "{\"startDateTime\":\"" + start + "\",\"endDateTime\":\"" + end + "\",\"reason\":\"Consultation\"}";
    }
}
