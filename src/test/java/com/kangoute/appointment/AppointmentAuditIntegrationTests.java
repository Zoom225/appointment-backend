package com.kangoute.appointment;

import com.kangoute.appointment.controller.AdminAppointmentController;
import com.kangoute.appointment.dto.request.AppointmentUpdateRequest;
import com.kangoute.appointment.dto.response.AppointmentAuditResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentAuditAction;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.AppointmentAuditService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class AppointmentAuditIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AdminAppointmentController adminAppointmentController;

    @Autowired
    private AppointmentAuditService appointmentAuditService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appointmentActionsAreRecordedAndVisibleToAdmin() {
        User owner = createUser("audit.owner@example.com");
        authenticateAs(owner);

        LocalDate date = LocalDate.of(2026, 8, 10);
        Appointment created = appointmentService.createAppointment(Appointment.builder()
                .user(owner)
                .startDateTime(date.atTime(9, 0))
                .endDateTime(date.atTime(9, 30))
                .reason("Initial")
                .build());

        AppointmentUpdateRequest updateRequest = new AppointmentUpdateRequest();
        updateRequest.setReason("Updated");
        updateRequest.setStartDateTime(date.atTime(10, 0));
        updateRequest.setEndDateTime(date.atTime(10, 30));
        appointmentService.updateAppointment(created.getId(), toAppointment(updateRequest, owner));
        appointmentService.updateStatus(created.getId(), AppointmentStatus.CONFIRMED);
        appointmentService.cancelAppointment(created.getId());

        authenticateAs(adminUser());
        List<AppointmentAuditResponse> history = adminAppointmentController.getHistory(created.getId());

        assertEquals(4, history.size());
        assertEquals(AppointmentAuditAction.CANCELLED, history.get(0).getAction());
        assertEquals(AppointmentAuditAction.STATUS_CHANGED, history.get(1).getAction());
        assertEquals(AppointmentAuditAction.UPDATED, history.get(2).getAction());
        assertEquals(AppointmentAuditAction.CREATED, history.get(3).getAction());
        assertEquals("audit.owner@example.com", history.get(0).getActorEmail());
    }

    @Test
    void auditDefaultsToSystemWhenNoAuthenticationIsPresent() {
        User owner = createUser("system.audit@example.com");
        LocalDate date = LocalDate.of(2026, 8, 11);

        Appointment created = appointmentService.createAppointment(Appointment.builder()
                .user(owner)
                .startDateTime(date.atTime(9, 0))
                .endDateTime(date.atTime(9, 30))
                .reason("No auth")
                .build());

        List<AppointmentAuditResponse> history = appointmentAuditService.getHistory(created.getId());
        assertEquals("SYSTEM", history.get(0).getActorEmail());
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
        admin.setEmail("admin.audit@example.com");
        admin.setPassword("secret123");

        com.kangoute.appointment.entity.Role role = new com.kangoute.appointment.entity.Role();
        role.setName(com.kangoute.appointment.enums.RoleName.ROLE_ADMIN);
        admin.setRoles(java.util.Set.of(role));
        return admin;
    }

    private Appointment toAppointment(AppointmentUpdateRequest request, User user) {
        return Appointment.builder()
                .user(user)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .reason(request.getReason())
                .build();
    }
}
