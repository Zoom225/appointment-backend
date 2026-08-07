package com.kangoute.appointment;

import com.kangoute.appointment.controller.AdminNotificationController;
import com.kangoute.appointment.controller.NotificationController;
import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class NotificationPaginationIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private AdminNotificationController adminNotificationController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userNotificationsCanBePaginatedAndFiltered() {
        User user = createUser("notif.page@example.com");
        authenticateAs(user);
        LocalDate date = LocalDate.of(2026, 8, 10);

        appointmentService.createAppointment(Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(9, 0))
                .endDateTime(date.atTime(9, 30))
                .reason("One")
                .build());
        appointmentService.createAppointment(Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(10, 0))
                .endDateTime(date.atTime(10, 30))
                .reason("Two")
                .build());

        Page<AppointmentNotificationResponse> firstPage = notificationController.getMyNotifications(
                PageRequest.of(0, 1),
                null,
                null,
                null,
                null
        );

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(1, firstPage.getContent().size());

        Page<AppointmentNotificationResponse> createdOnly = notificationController.getMyNotifications(
                PageRequest.of(0, 10),
                AppointmentNotificationType.CREATED,
                null,
                null,
                null
        );

        assertEquals(2, createdOnly.getTotalElements());

        AppointmentNotificationResponse read = notificationController.markAsRead(firstPage.getContent().get(0).getId());
        assertEquals(read.getId(), firstPage.getContent().get(0).getId());

        Page<AppointmentNotificationResponse> unreadOnly = notificationController.getMyNotifications(
                PageRequest.of(0, 10),
                null,
                true,
                null,
                null
        );

        assertEquals(1, unreadOnly.getTotalElements());
    }

    @Test
    void adminNotificationsCanBeFilteredByRecipientAndDate() {
        User userA = createUser("admin.a@example.com");
        User userB = createUser("admin.b@example.com");
        authenticateAs(userA);
        LocalDate date = LocalDate.of(2026, 8, 11);

        appointmentService.createAppointment(Appointment.builder()
                .user(userA)
                .startDateTime(date.atTime(11, 0))
                .endDateTime(date.atTime(11, 30))
                .reason("A")
                .build());

        authenticateAs(userB);
        appointmentService.createAppointment(Appointment.builder()
                .user(userB)
                .startDateTime(date.atTime(12, 0))
                .endDateTime(date.atTime(12, 30))
                .reason("B")
                .build());

        authenticateAs(adminUser());
        Page<AppointmentNotificationResponse> filtered = adminNotificationController.getAllNotifications(
                PageRequest.of(0, 10),
                userA.getId(),
                AppointmentNotificationType.CREATED,
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        assertEquals(1, filtered.getTotalElements());
        assertEquals(userA.getId(), filtered.getContent().get(0).getRecipientId());
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }

    private void authenticateAs(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, user.getPassword(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User adminUser() {
        User admin = new User();
        admin.setId(999L);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin.page@example.com");
        admin.setPassword("secret123");
        com.kangoute.appointment.entity.Role role = new com.kangoute.appointment.entity.Role();
        role.setName(com.kangoute.appointment.enums.RoleName.ROLE_ADMIN);
        admin.setRoles(java.util.Set.of(role));
        return admin;
    }
}
