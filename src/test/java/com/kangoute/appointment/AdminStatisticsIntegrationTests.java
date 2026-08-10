package com.kangoute.appointment;

import com.kangoute.appointment.controller.AdminStatisticsController;
import com.kangoute.appointment.dto.response.AdminStatisticsResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.Role;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.exception.InvalidStatisticsPeriodException;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AdminStatisticsIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AdminStatisticsController adminStatisticsController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminStatisticsExposeTotalsAndPeriodCounts() {
        User userA = createUser("stats.a@example.com");
        User userB = createUser("stats.b@example.com");
        authenticateAs(adminUser());

        LocalDate date = LocalDate.of(2026, 8, 3);

        Appointment appointmentA = appointmentService.createAppointment(Appointment.builder()
                .user(userA)
                .startDateTime(date.atTime(10, 0))
                .endDateTime(date.atTime(10, 30))
                .reason("A")
                .build());
        appointmentService.updateStatus(appointmentA.getId(), AppointmentStatus.CONFIRMED);

        appointmentService.createAppointment(Appointment.builder()
                .user(userA)
                .startDateTime(date.atTime(11, 0))
                .endDateTime(date.atTime(11, 30))
                .reason("B")
                .build());

        Appointment appointmentB = appointmentService.createAppointment(Appointment.builder()
                .user(userB)
                .startDateTime(date.atTime(12, 0))
                .endDateTime(date.atTime(12, 30))
                .reason("C")
                .build());
        appointmentService.cancelAppointment(appointmentB.getId());

        LocalDateTime periodFrom = date.atStartOfDay();
        LocalDateTime periodTo = date.plusDays(1).atStartOfDay();
        AdminStatisticsResponse response = adminStatisticsController.getStatistics(periodFrom, periodTo);

        assertEquals(2, response.getTotalUsers());
        assertEquals(2, response.getActiveUsersLast30Days());
        assertEquals(3, response.getTotalAppointments());
        assertEquals(3, response.getAppointmentsInPeriod());
        assertEquals(1, response.getPendingAppointments());
        assertEquals(1, response.getConfirmedAppointments());
        assertEquals(1, response.getCancelledAppointments());
        assertNotNull(response.getActiveSince());
        assertEquals(periodFrom, response.getPeriodFrom());
        assertEquals(periodTo, response.getPeriodTo());
    }

    @Test
    void invalidStatisticsPeriodIsRejected() {
        authenticateAs(adminUser());

        InvalidStatisticsPeriodException exception = assertThrows(InvalidStatisticsPeriodException.class, () -> adminStatisticsController.getStatistics(
                LocalDateTime.of(2026, 8, 10, 0, 0),
                LocalDateTime.of(2026, 8, 9, 0, 0)
        ));

        assertEquals("La date de debut de la periode statistique doit etre avant la date de fin", exception.getMessage());
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
        admin.setEmail("admin.stats@example.com");
        admin.setPassword("secret123");
        Role role = new Role();
        role.setName(RoleName.ROLE_ADMIN);
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
