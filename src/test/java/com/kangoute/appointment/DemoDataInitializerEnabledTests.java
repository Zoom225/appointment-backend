package com.kangoute.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangoute.appointment.config.DemoDataInitializer;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.demo.enabled=true",
        "app.demo.email=demo@gestion-rendez-vous.com",
        "app.demo.password=Demo2026!",
        "app.demo.first-name=Demo",
        "app.demo.last-name=Recruiter"
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
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void demoUserAndAppointmentsAreCreatedOnStartup() {
        User demoUser = getDemoUser();
        assertNotNull(demoUser.getId());
        assertEquals("Demo", demoUser.getFirstName());
        assertEquals("Recruiter", demoUser.getLastName());
        assertFalse(demoUser.getRoles().isEmpty());
        assertTrue(demoUser.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_USER));
        assertFalse(demoUser.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN));
        assertNotEquals("Demo2026!", demoUser.getPassword());
        assertTrue(passwordEncoder.matches("Demo2026!", demoUser.getPassword()));

        List<Appointment> appointments = appointmentRepository.findByUserId(demoUser.getId());
        assertEquals(5, appointments.size());
        assertTrue(appointments.stream().allMatch(appointment -> appointment.getUser().getId().equals(demoUser.getId())));

        Set<AppointmentStatus> statuses = appointments.stream()
                .map(Appointment::getStatus)
                .collect(Collectors.toSet());

        assertTrue(statuses.containsAll(Set.of(
                AppointmentStatus.PENDING,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.COMPLETED
        )));
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
                .filter(user -> user.getEmail().equals("demo@gestion-rendez-vous.com"))
                .count());
        assertEquals(5, appointmentRepository.findByUserId(demoUser.getId()).size());
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
    }

    @Test
    void demoLoginWorks() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@gestion-rendez-vous.com",
                                  "password": "Demo2026!"
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

    private User getDemoUser() {
        return userRepository.findByEmail("demo@gestion-rendez-vous.com")
                .orElseThrow();
    }
}
