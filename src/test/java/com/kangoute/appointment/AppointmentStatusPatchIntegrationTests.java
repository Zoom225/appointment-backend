package com.kangoute.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.service.RoleService;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class AppointmentStatusPatchIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void patchAppointmentStatusConfirmedReturns200AndPersistsStatus() throws Exception {
        User owner = createUser("patch-owner@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.id").value(appointmentId.intValue()));

        mockMvc.perform(get("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void patchScheduledAppointmentToConfirmedReturns200() throws Exception {
        User owner = createUser("patch-scheduled-confirmed@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "scheduled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void patchConfirmedAppointmentToCompletedReturns200AndPersistsStatus() throws Exception {
        User owner = createUser("patch-completed@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "completed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void userCannotPatchAnotherUsersAppointmentStatus() throws Exception {
        User owner = createUser("patch-owner-denied@example.com");
        User intruder = createUser("patch-intruder@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        String intruderToken = login(intruder.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "confirmed"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPatchAppointmentStatus() throws Exception {
        User owner = createUser("patch-owner-admin@example.com");
        User admin = createAdmin("patch-admin@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        String adminToken = login(admin.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void invalidStatusReturnsBadRequest() throws Exception {
        User owner = createUser("patch-invalid@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "unknown"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelEndpointStillWorks() throws Exception {
        User owner = createUser("patch-cancel@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void patchScheduledAppointmentToCancelledReturns200() throws Exception {
        User owner = createUser("patch-scheduled-cancelled@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "scheduled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(patch("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "cancelled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void putEndpointStillWorksAfterStatusPatchSupport() throws Exception {
        User owner = createUser("patch-put@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());
        LocalDate date = nextWorkingDate();

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDateTime": "%s",
                                  "endDateTime": "%s",
                                  "reason": "Updated reason"
                                }
                                """.formatted(date.atTime(10, 0), date.atTime(10, 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Updated reason"));
    }

    @Test
    void postAppointmentRejectsPastStartDate() throws Exception {
        User owner = createUser("appointment-past-create@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDateTime": "2026-08-10T10:00:00",
                                  "endDateTime": "2026-08-10T10:30:00",
                                  "reason": "Past start",
                                  "userId": %d
                                }
                                """.formatted(owner.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putAppointmentRejectsEndBeforeStart() throws Exception {
        User owner = createUser("appointment-invalid-update@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        Long appointmentId = createAppointment(owner.getId());
        LocalDate date = nextWorkingDate();

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDateTime": "%s",
                                  "endDateTime": "%s",
                                  "reason": "Invalid range"
                                }
                                """.formatted(date.atTime(11, 0), date.atTime(10, 30))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postAppointmentWithValidDatesReturnsCreated() throws Exception {
        User owner = createUser("appointment-valid-create@example.com");
        String ownerToken = login(owner.getEmail(), "secret123");
        LocalDate date = nextWorkingDate();

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDateTime": "%s",
                                  "endDateTime": "%s",
                                  "reason": "Valid create",
                                  "userId": %d
                                }
                                """.formatted(date.atTime(12, 0), date.atTime(12, 30), owner.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("Valid create"));
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }

    private User createAdmin(String email) {
        User user = createUser(email);
        User adminUpdate = new User();
        adminUpdate.setFirstName(user.getFirstName());
        adminUpdate.setLastName(user.getLastName());
        adminUpdate.setEmail(user.getEmail());
        adminUpdate.setPassword("secret123");
        adminUpdate.setRoles(new java.util.HashSet<>(Set.of(roleService.createRole(RoleName.ROLE_ADMIN))));
        return userService.updateUser(user.getId(), adminUpdate);
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("token").asText();
    }

    private Long createAppointment(Long userId) {
        Appointment appointment = Appointment.builder()
                .user(userService.getUserById(userId))
                .startDateTime(java.time.LocalDate.of(2026, 8, 10).atTime(9, 0))
                .endDateTime(java.time.LocalDate.of(2026, 8, 10).atTime(9, 30))
                .reason("Consultation")
                .build();

        return appointmentService.createAppointment(appointment).getId();
    }

    private LocalDate nextWorkingDate() {
        LocalDate date = LocalDate.now().plusDays(14);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
