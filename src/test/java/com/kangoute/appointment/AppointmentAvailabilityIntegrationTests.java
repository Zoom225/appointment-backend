package com.kangoute.appointment;

import com.kangoute.appointment.dto.response.AppointmentAvailabilitySlotResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AppointmentAvailabilityIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentAvailabilityService appointmentAvailabilityService;

    @Test
    void createAppointmentRejectsOutsideWorkingHours() {
        User user = createUser("outside.hours@example.com");
        LocalDate date = LocalDate.of(2026, 8, 10);

        Appointment appointment = Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(8, 30))
                .endDateTime(date.atTime(9, 0))
                .reason("Consultation")
                .build();

        assertThrows(RuntimeException.class, () -> appointmentService.createAppointment(appointment));
    }

    @Test
    void getAvailableSlotsSkipsBusySlot() {
        User user = createUser("slots@example.com");
        LocalDate date = LocalDate.of(2026, 8, 10);

        Appointment booked = Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(10, 0))
                .endDateTime(date.atTime(10, 30))
                .reason("Booked")
                .build();
        appointmentService.createAppointment(booked);

        List<AppointmentAvailabilitySlotResponse> slots = appointmentAvailabilityService.getAvailableSlots(user.getId(), date);

        assertEquals(17, slots.size());
        assertFalse(slots.stream().anyMatch(slot ->
                slot.getStartDateTime().equals(date.atTime(10, 0))
                        && slot.getEndDateTime().equals(date.atTime(10, 30))
        ));
    }

    @Test
    void createAppointmentWithinWorkingHoursSavesPendingAppointment() {
        User user = createUser("valid.hours@example.com");
        LocalDate date = LocalDate.of(2026, 8, 10);

        Appointment appointment = Appointment.builder()
                .user(user)
                .startDateTime(date.atTime(9, 0))
                .endDateTime(date.atTime(9, 30))
                .reason("Checkup")
                .build();

        Appointment saved = appointmentService.createAppointment(appointment);

        assertEquals(AppointmentStatus.PENDING, saved.getStatus());
        assertEquals(date.atTime(9, 0), saved.getStartDateTime());
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("secret123");
        return userService.createUser(user);
    }
}
