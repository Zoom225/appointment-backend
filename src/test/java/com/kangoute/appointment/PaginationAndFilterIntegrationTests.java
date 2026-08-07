package com.kangoute.appointment;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.RoleService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PaginationAndFilterIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void usersCanBePaginatedAndFilteredByQueryAndRole() {
        createUser("alice", "Alpha", "alice.alpha@example.com");
        createUser("bob", "Beta", "bob.beta@example.com");
        createAdminUser("admin.search@example.com");

        var page = userService.getAllUsers(
                PageRequest.of(0, 2, Sort.by("id").ascending()),
                "al",
                null
        );

        assertEquals(1, page.getContent().size());
        assertTrue(page.getContent().get(0).getEmail().contains("alice"));

        var admins = userService.getAllUsers(
                PageRequest.of(0, 10, Sort.by("id").ascending()),
                null,
                RoleName.ROLE_ADMIN
        );

        assertEquals(1, admins.getTotalElements());
        assertEquals("admin.search@example.com", admins.getContent().get(0).getEmail());
    }

    @Test
    void appointmentsCanBePaginatedAndFilteredByStatusAndDateRange() {
        User user = createUser("appointments", "Owner", "owner.appointments@example.com");
        LocalDate date = LocalDate.of(2026, 8, 10);

        Appointment first = appointmentService.createAppointment(Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(9, 0))
                .endDateTime(date.atTime(9, 30))
                .reason("First")
                .build());

        Appointment second = appointmentService.createAppointment(Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(10, 0))
                .endDateTime(date.atTime(10, 30))
                .reason("Second")
                .build());
        appointmentService.cancelAppointment(second.getId());

        var paged = appointmentService.getAppointmentsByUserId(
                user.getId(),
                PageRequest.of(0, 1, Sort.by("startDateTime").ascending()),
                null,
                null,
                null
        );

        assertEquals(2, paged.getTotalElements());
        assertEquals(1, paged.getContent().size());
        assertEquals(first.getId(), paged.getContent().get(0).getId());

        var cancelled = appointmentService.getAllAppointments(
                PageRequest.of(0, 10, Sort.by("id").ascending()),
                user.getId(),
                AppointmentStatus.CANCELLED,
                null,
                null
        );

        assertEquals(1, cancelled.getTotalElements());
        assertEquals(second.getId(), cancelled.getContent().get(0).getId());

        var dateWindow = appointmentService.getAllAppointments(
                PageRequest.of(0, 10, Sort.by("id").ascending()),
                user.getId(),
                null,
                date.atTime(9, 15),
                date.atTime(9, 45)
        );

        assertEquals(1, dateWindow.getTotalElements());
        assertEquals(first.getId(), dateWindow.getContent().get(0).getId());
    }

    private User createUser(String firstName, String lastName, String email) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }

    private void createAdminUser(String email) {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode("secret123"));
        admin.setRoles(java.util.Set.of(roleService.createRole(RoleName.ROLE_ADMIN)));
        userRepository.save(admin);
    }
}
