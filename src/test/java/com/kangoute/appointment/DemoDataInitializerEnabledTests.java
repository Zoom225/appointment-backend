package com.kangoute.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangoute.appointment.config.DemoDataInitializer;
import com.kangoute.appointment.config.DemoProperties;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.service.RoleService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-data-enabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "app.demo.enabled=true",
        "app.demo.user.email=demo.user@appointment.local",
        "app.demo.user.password=DemoUser2026!",
        "app.demo.user.first-name=Demo",
        "app.demo.user.last-name=Utilisateur",
        "app.demo.admin.email=demo.admin@appointment.local",
        "app.demo.admin.password=DemoAdmin2026!",
        "app.demo.admin.first-name=Demo",
        "app.demo.admin.last-name=Administrateur"
})
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoDataInitializerEnabledTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private DemoProperties demoProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void demoUserAndAppointmentsAreCreatedOnStartup() {
        User demoUser = getDemoUser();
        assertNotNull(demoUser.getId());
        assertEquals(DemoAccountType.USER, demoUser.getDemoAccountType());
        assertEquals("Demo", demoUser.getFirstName());
        assertEquals("Utilisateur", demoUser.getLastName());
        assertFalse(demoUser.getRoles().isEmpty());
        assertTrue(demoUser.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_USER));
        assertFalse(demoUser.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN));
        assertNotEquals("DemoUser2026!", demoUser.getPassword());
        assertTrue(passwordEncoder.matches("DemoUser2026!", demoUser.getPassword()));

        List<Appointment> appointments = appointmentRepository.findByUserId(demoUser.getId());
        assertTrue(appointments.isEmpty());
        User demoAdmin = getDemoAdmin();
        assertEquals(DemoAccountType.ADMIN, demoAdmin.getDemoAccountType());
        assertEquals("Administrateur", demoAdmin.getLastName());
        assertEquals(Set.of(RoleName.ROLE_ADMIN), demoAdmin.getRoles().stream()
                .map(role -> role.getName()).collect(Collectors.toSet()));
        assertNotEquals("DemoAdmin2026!", demoAdmin.getPassword());
        assertTrue(passwordEncoder.matches("DemoAdmin2026!", demoAdmin.getPassword()));
    }

    @Test
    void rerunningInitializerDoesNotDuplicateDemoData() throws Exception {
        User demoUser = getDemoUser();
        long usersBefore = userRepository.count();
        long appointmentsBefore = appointmentRepository.count();

        demoDataInitializer.run(null);

        assertEquals(usersBefore, userRepository.count());
        assertEquals(appointmentsBefore, appointmentRepository.count());
        assertEquals(1, userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals("demo.user@appointment.local"))
                .count());
        assertEquals(1, userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals("demo.admin@appointment.local")).count());
        assertTrue(appointmentRepository.findByUserId(demoUser.getId()).isEmpty());
    }

    @Test
    void existingUsersAreNotModified() throws Exception {
        User existing = new User();
        existing.setFirstName("Existing");
        existing.setLastName("User");
        existing.setEmail("existing@example.com");
        existing.setPassword("secret123");
        User created = userService.createUser(existing);
        String originalPassword = created.getPassword();

        demoDataInitializer.run(null);

        User reloaded = userRepository.findByEmail("existing@example.com").orElseThrow();
        assertEquals(created.getId(), reloaded.getId());
        assertEquals("Existing", reloaded.getFirstName());
        assertEquals("User", reloaded.getLastName());
        assertEquals("existing@example.com", reloaded.getEmail());
        assertEquals(originalPassword, reloaded.getPassword());
        assertEquals(DemoAccountType.NONE, reloaded.getDemoAccountType());
    }

    @Test
    void existingDemoUserIsNotOverwritten() {
        User legacyDemoUser = getDemoUser();
        legacyDemoUser.setPassword("unchanged-existing-password");
        legacyDemoUser.setRoles(new HashSet<>());
        userRepository.save(legacyDemoUser);

        demoDataInitializer.run(null);

        User updated = getDemoUser();
        assertEquals("unchanged-existing-password", updated.getPassword());
        assertTrue(updated.getRoles().isEmpty());
        assertTrue(appointmentRepository.findByUserId(updated.getId()).isEmpty());
    }

    @Test
    void demoLoginWorks() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo.user@appointment.local",
                                  "password": "DemoUser2026!"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertTrue(jsonNode.get("token").asText().length() > 10);
    }

    @Test
    void demoAdminLoginAndRoleProtectedEndpointsWork() throws Exception {
        String userToken = login("demo.user@appointment.local", "DemoUser2026!");
        String adminToken = login("demo.admin@appointment.local", "DemoAdmin2026!");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        for (String path : List.of("/api/admin/users", "/api/admin/appointments",
                "/api/admin/statistics", "/api/admin/notifications")) {
            mockMvc.perform(MockMvcRequestBuilders.get(path)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }

    @Test
    void demoBookingCreatesUserNotificationVisibleToAdmin() throws Exception {
        String userToken = login("demo.user@appointment.local", "DemoUser2026!");
        String adminToken = login("demo.admin@appointment.local", "DemoAdmin2026!");
        java.time.LocalDate day = java.time.LocalDate.now().plusDays(2);
        while (day.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) day = day.plusDays(1);
        String date = day.toString();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/appointments/availability?date=" + date)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(MockMvcResultMatchers.status().isOk());
        String start = day.atTime(10, 0).toString();
        String end = day.atTime(10, 30).toString();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/appointments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "startDateTime", start, "endDateTime", end, "reason", "Portfolio demo",
                                "contactFirstName", "Demo", "contactLastName", "Utilisateur", "contactEmail", "contact@example.com"))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("PENDING"));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].title").value("Nouveau rendez-vous"));
    }

    @Test
    void demoAdminListsOnlyDemoAppointmentsEvenWithFiltersAndPagination() throws Exception {
        Fixture fixture = createMixedBookings();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments")
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(fixture.demoAppointmentId()));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments?query=real.user@example.com")
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        String realReference = appointmentRepository.findById(fixture.realAppointmentId()).orElseThrow().getPublicReference();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments").param("query", realReference)
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments").param("query", "contact@example.com")
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(fixture.demoAppointmentId()));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments?page=1&size=1")
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(0));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments?userId=" + fixture.realUserId())
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments")
                        .header("Authorization", bearer(fixture.realAdminToken())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
    }

    @Test
    void demoAdminCannotReadChangeOrAuditRealAppointmentById() throws Exception {
        Fixture fixture = createMixedBookings();
        long id = fixture.realAppointmentId();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments/{id}", id)
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments/{id}/history", id)
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/appointments/{id}/status", id)
                        .header("Authorization", bearer(fixture.demoAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/appointments/{id}", id)
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments/{id}", id)
                        .header("Authorization", bearer(fixture.realAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/appointments/{id}/status", id)
                        .header("Authorization", bearer(fixture.realAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void demoAdminStatisticsUsersAndNotificationsAreScoped() throws Exception {
        Fixture fixture = createMixedBookings();
        String demo = bearer(fixture.demoAdminToken());
        String real = bearer(fixture.realAdminToken());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/statistics").header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalAppointments").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.pendingAppointments").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalUsers").value(2));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/statistics").header("Authorization", real))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalAppointments").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalUsers").value(4));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users").header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users").header("Authorization", real))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(4));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users/{id}", fixture.realUserId())
                        .header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}", fixture.realUserId())
                        .header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.put("/api/admin/users/{id}", getDemoUser().getId())
                        .header("Authorization", demo).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Demo","lastName":"Utilisateur",
                                 "email":"demo.user@appointment.local","roles":["ROLE_ADMIN"]}
                                """))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{id}", fixture.realUserId())
                        .header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/notifications").header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/notifications").header("Authorization", real))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(5));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/notifications?recipientId=" + fixture.realUserId())
                        .header("Authorization", demo))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void demoAdminCanConfirmDemoBookingAndDemoUserSeesNewStatus() throws Exception {
        Fixture fixture = createMixedBookings();
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/appointments/{id}/status",
                        fixture.demoAppointmentId())
                        .header("Authorization", bearer(fixture.demoAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/appointments/me/upcoming")
                        .header("Authorization", bearer(fixture.demoUserToken())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("CONFIRMED"));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments/{id}/history",
                        fixture.demoAppointmentId())
                        .header("Authorization", bearer(fixture.demoAdminToken())))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users")
                        .header("Authorization", bearer(fixture.demoUserToken())))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void changingConfiguredAdminEmailKeepsOldDemoAdminRestrictedAndRealAdminGlobal() throws Exception {
        Fixture fixture = createMixedBookings();
        String original = demoProperties.getAdmin().getEmail();
        Long demoAdminId = getDemoAdmin().getId();
        demoProperties.getAdmin().setEmail("real.admin@example.com");
        try {
            demoDataInitializer.run(null);
            assertEquals(demoAdminId, getDemoAdmin().getId());
            assertEquals(DemoAccountType.ADMIN, getDemoAdmin().getDemoAccountType());
            assertEquals(DemoAccountType.NONE,
                    userRepository.findByEmail("real.admin@example.com").orElseThrow().getDemoAccountType());
            assertTrue(userRepository.findByDemoAccountType(DemoAccountType.ADMIN).size() == 1);
            login("demo.admin@appointment.local", "DemoAdmin2026!");
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments")
                            .header("Authorization", bearer(fixture.demoAdminToken())))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments/{id}", fixture.realAppointmentId())
                            .header("Authorization", bearer(fixture.demoAdminToken())))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments")
                            .header("Authorization", bearer(fixture.realAdminToken())))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        } finally {
            demoProperties.getAdmin().setEmail(original);
        }
    }

    @Test
    void changingConfiguredUserEmailDoesNotChangeDemoOwner() throws Exception {
        Fixture fixture = createMixedBookings();
        String original = demoProperties.getUser().getEmail();
        demoProperties.getUser().setEmail("real.user@example.com");
        try {
            demoDataInitializer.run(null);
            assertEquals(DemoAccountType.USER, getDemoUser().getDemoAccountType());
            assertEquals(DemoAccountType.NONE,
                    userRepository.findByEmail("real.user@example.com").orElseThrow().getDemoAccountType());
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments")
                            .header("Authorization", bearer(fixture.demoAdminToken())))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(fixture.demoAppointmentId()));
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/appointments/{id}", fixture.realAppointmentId())
                            .header("Authorization", bearer(fixture.demoAdminToken())))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        } finally {
            demoProperties.getUser().setEmail(original);
        }
    }

    @Test
    void disablingDemoBlocksMarkedAccountsEvenAfterConfiguredEmailChanges() throws Exception {
        String oldAdminEmail = demoProperties.getAdmin().getEmail();
        String oldUserEmail = demoProperties.getUser().getEmail();
        demoProperties.getAdmin().setEmail("new.demo.admin@example.com");
        demoProperties.getUser().setEmail("new.demo.user@example.com");
        demoProperties.setEnabled(false);
        try {
            for (String email : List.of("demo.admin@appointment.local", "demo.user@appointment.local")) {
                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(java.util.Map.of(
                                        "email", email, "password", email.contains("admin")
                                                ? "DemoAdmin2026!" : "DemoUser2026!"))))
                        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
            }
        } finally {
            demoProperties.setEnabled(true);
            demoProperties.getAdmin().setEmail(oldAdminEmail);
            demoProperties.getUser().setEmail(oldUserEmail);
        }
    }

    private Fixture createMixedBookings() throws Exception {
        User realUser = createAccount("real.user@example.com", RoleName.ROLE_USER);
        createAccount("real.admin@example.com", RoleName.ROLE_ADMIN);
        String demoUserToken = login("demo.user@appointment.local", "DemoUser2026!");
        String demoAdminToken = login("demo.admin@appointment.local", "DemoAdmin2026!");
        String realUserToken = login("real.user@example.com", "TestOnly2026!");
        String realAdminToken = login("real.admin@example.com", "TestOnly2026!");
        java.time.LocalDate day = java.time.LocalDate.now().plusDays(3);
        while (day.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) day = day.plusDays(1);
        long demoId = book(demoUserToken, day, 10);
        long realId = book(realUserToken, day, 11);
        return new Fixture(demoUserToken, demoAdminToken, realAdminToken, realUser.getId(), demoId, realId);
    }

    private User createAccount(String email, RoleName role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Real");
        user.setLastName("Account");
        user.setPassword("TestOnly2026!");
        User saved = userService.createUser(user);
        if (role == RoleName.ROLE_ADMIN) {
            saved.setRoles(new HashSet<>(Set.of(roleService.createRole(RoleName.ROLE_ADMIN))));
            saved = userRepository.saveAndFlush(saved);
        }
        return saved;
    }

    private long book(String token, java.time.LocalDate day, int hour) throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/appointments")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "startDateTime", day.atTime(hour, 0).toString(),
                                "endDateTime", day.atTime(hour, 30).toString(),
                                "reason", "Test de portee admin",
                                "contactFirstName", "Demo", "contactLastName", "Utilisateur", "contactEmail", "contact@example.com"))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String bearer(String token) { return "Bearer " + token; }

    private record Fixture(String demoUserToken, String demoAdminToken, String realAdminToken,
                           Long realUserId, long demoAppointmentId, long realAppointmentId) { }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email, "password", password))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private User getDemoUser() {
        return userRepository.findByEmail("demo.user@appointment.local")
                .orElseThrow();
    }

    private User getDemoAdmin() {
        return userRepository.findByEmail("demo.admin@appointment.local").orElseThrow();
    }
}
