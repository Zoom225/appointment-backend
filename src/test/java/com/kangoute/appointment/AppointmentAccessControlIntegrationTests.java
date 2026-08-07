package com.kangoute.appointment;

import com.kangoute.appointment.controller.AppointmentController;
import com.kangoute.appointment.controller.UserController;
import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.dto.request.AppointmentUpdateRequest;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AppointmentAccessControlIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentController appointmentController;

    @Autowired
    private UserController userController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userCannotCreateAppointmentForAnotherUser() {
        User owner = createUser("owner@example.com");
        User other = createUser("other@example.com");
        authenticateAs(owner);

        AppointmentCreateRequest request = new AppointmentCreateRequest();
        request.setUserId(other.getId());
        request.setReason("Consultation");
        request.setStartDateTime(LocalDate.of(2026, 8, 10).atTime(9, 0));
        request.setEndDateTime(LocalDate.of(2026, 8, 10).atTime(9, 30));

        assertThrows(AccessDeniedException.class, () -> appointmentController.createAppointment(request));
    }

    @Test
    void userOnlySeesOwnAppointmentsAndProfile() {
        User owner = createUser("owner.list@example.com");
        User other = createUser("other.list@example.com");
        Appointment ownerAppointment = createAppointment(owner, "Owner");
        Appointment otherAppointment = createAppointment(other, "Other");
        authenticateAs(owner);

        assertEquals(1, appointmentController.getAppointments(PageRequest.of(0, 10), null, null, null, null).getTotalElements());
        assertEquals(ownerAppointment.getId(), appointmentController.getAppointmentById(ownerAppointment.getId()).getId());
        assertThrows(AccessDeniedException.class, () -> appointmentController.getAppointmentById(otherAppointment.getId()));
        assertThrows(AccessDeniedException.class, () -> userController.getUserByEmail(other.getEmail()));
    }

    @Test
    void userCannotUpdateOrCancelAnotherUserAppointment() {
        User owner = createUser("owner.edit@example.com");
        User other = createUser("other.edit@example.com");
        Appointment otherAppointment = createAppointment(other, "Other");
        authenticateAs(owner);

        AppointmentUpdateRequest updateRequest = new AppointmentUpdateRequest();
        updateRequest.setReason("Updated");
        updateRequest.setStartDateTime(LocalDate.of(2026, 8, 10).atTime(10, 0));
        updateRequest.setEndDateTime(LocalDate.of(2026, 8, 10).atTime(10, 30));

        assertThrows(AccessDeniedException.class, () -> appointmentController.updateAppointment(otherAppointment.getId(), updateRequest));
        assertThrows(AccessDeniedException.class, () -> appointmentController.cancelAppointment(otherAppointment.getId()));
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }

    private Appointment createAppointment(User user, String reason) {
        Appointment appointment = Appointment.builder()
                .user(user)
                .startDateTime(LocalDate.of(2026, 8, 10).atTime(11, 0))
                .endDateTime(LocalDate.of(2026, 8, 10).atTime(11, 30))
                .reason(reason)
                .build();
        return appointmentService.createAppointment(appointment);
    }

    private void authenticateAs(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, user.getPassword(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
