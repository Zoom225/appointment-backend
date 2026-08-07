package com.kangoute.appointment;

import com.kangoute.appointment.controller.AdminAppointmentController;
import com.kangoute.appointment.controller.NotificationController;
import com.kangoute.appointment.dto.request.AppointmentUpdateRequest;
import com.kangoute.appointment.dto.request.AppointmentStatusUpdateRequest;
import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentNotificationService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class AppointmentNotificationIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentNotificationService appointmentNotificationService;

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private AdminAppointmentController adminAppointmentController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lifecycleActionsCreateNotificationsAndCanBeMarkedRead() {
        User owner = createUser("notify.owner@example.com");
        authenticateAs(owner);

        LocalDate date = LocalDate.of(2026, 8, 10);
        Appointment appointment = appointmentService.createAppointment(Appointment.builder()
                .user(owner)
                .startDateTime(date.atTime(11, 0))
                .endDateTime(date.atTime(11, 30))
                .reason("Initial")
                .build());

        Page<AppointmentNotificationResponse> createdNotifications = notificationController.getMyNotifications(
                PageRequest.of(0, 10),
                null,
                null,
                null,
                null
        );
        assertEquals(1, createdNotifications.getTotalElements());
        assertEquals(AppointmentNotificationType.CREATED, createdNotifications.getContent().get(0).getType());

        AppointmentUpdateRequest updateRequest = new AppointmentUpdateRequest();
        updateRequest.setReason("Updated");
        updateRequest.setStartDateTime(date.atTime(12, 0));
        updateRequest.setEndDateTime(date.atTime(12, 30));
        authenticateAs(owner);
        appointmentService.updateAppointment(appointment.getId(), toAppointment(owner, updateRequest));

        authenticateAs(adminUser());
        adminAppointmentController.updateStatus(appointment.getId(), statusRequest(AppointmentStatus.CONFIRMED));

        authenticateAs(owner);
        appointmentService.cancelAppointment(appointment.getId());

        Page<AppointmentNotificationResponse> notifications = notificationController.getMyNotifications(
                PageRequest.of(0, 10),
                null,
                null,
                null,
                null
        );
        assertEquals(4, notifications.getTotalElements());
        assertEquals(AppointmentNotificationType.CANCELLED, notifications.getContent().get(0).getType());
        assertEquals(AppointmentNotificationType.STATUS_CHANGED, notifications.getContent().get(1).getType());
        assertEquals(AppointmentNotificationType.UPDATED, notifications.getContent().get(2).getType());
        assertEquals(AppointmentNotificationType.CREATED, notifications.getContent().get(3).getType());

        AppointmentNotificationResponse read = notificationController.markAsRead(notifications.getContent().get(0).getId());
        assertNotNull(read.getReadAt());
    }

    @Test
    void reminderJobCreatesSingleReminderNotification() {
        User owner = createUser("reminder.owner@example.com");
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);
        Appointment appointment = appointmentService.createAppointment(Appointment.builder()
                .user(owner)
                .startDateTime(now.plusMinutes(60))
                .endDateTime(now.plusMinutes(90))
                .reason("Reminder")
                .build());

        appointmentNotificationService.sendDueReminders(now);
        appointmentNotificationService.sendDueReminders(now);

        List<AppointmentNotificationResponse> notifications = appointmentNotificationService.getMyNotifications(owner.getId());
        long reminderCount = notifications.stream()
                .filter(notification -> notification.getType() == AppointmentNotificationType.REMINDER)
                .count();

        assertEquals(1L, reminderCount);
        assertEquals(2, notifications.size());
        assertEquals(AppointmentNotificationType.REMINDER, notifications.get(0).getType());
        assertEquals(appointment.getId(), notifications.get(0).getAppointmentId());
    }

    private Appointment toAppointment(User user, AppointmentUpdateRequest request) {
        return Appointment.builder()
                .user(user)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .reason(request.getReason())
                .build();
    }

    private AppointmentStatusUpdateRequest statusRequest(AppointmentStatus status) {
        AppointmentStatusUpdateRequest request = new AppointmentStatusUpdateRequest();
        request.setStatus(status);
        return request;
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }

    private User adminUser() {
        User admin = new User();
        admin.setId(999L);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin.notify@example.com");
        admin.setPassword("secret123");
        com.kangoute.appointment.entity.Role role = new com.kangoute.appointment.entity.Role();
        role.setName(com.kangoute.appointment.enums.RoleName.ROLE_ADMIN);
        admin.setRoles(java.util.Set.of(role));
        return admin;
    }

    private void authenticateAs(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, user.getPassword(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
