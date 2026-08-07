package com.kangoute.appointment.impl;

import com.kangoute.appointment.config.AppointmentAvailabilityProperties;
import com.kangoute.appointment.dto.response.AppointmentAvailabilitySlotResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.exception.AppointmentOutsideAvailabilityException;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentAvailabilityServiceImpl implements AppointmentAvailabilityService {

    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final AppointmentAvailabilityProperties properties;

    @Override
    public void validateAppointmentWindow(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
            throw new AppointmentOutsideAvailabilityException("Appointment must start and end on the same day");
        }

        if (!properties.getWorkingDays().contains(startDateTime.getDayOfWeek())) {
            throw new AppointmentOutsideAvailabilityException("Appointment must be scheduled on a working day");
        }

        LocalTime startTime = startDateTime.toLocalTime();
        LocalTime endTime = endDateTime.toLocalTime();
        if (startTime.isBefore(properties.getWorkdayStart()) || endTime.isAfter(properties.getWorkdayEnd())) {
            throw new AppointmentOutsideAvailabilityException("Appointment must stay within working hours");
        }
    }

    @Override
    public List<AppointmentAvailabilitySlotResponse> getAvailableSlots(Long userId, LocalDate date) {
        userService.getUserById(userId);

        if (!properties.getWorkingDays().contains(date.getDayOfWeek())) {
            return List.of();
        }

        LocalDateTime windowStart = date.atTime(properties.getWorkdayStart());
        LocalDateTime windowEnd = date.atTime(properties.getWorkdayEnd());
        Duration slotDuration = Duration.ofMinutes(properties.getSlotMinutes());

        List<Appointment> appointments = appointmentRepository
                .findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(userId, windowEnd, windowStart)
                .stream()
                .sorted(Comparator.comparing(Appointment::getStartDateTime))
                .toList();

        List<AppointmentAvailabilitySlotResponse> slots = new java.util.ArrayList<>();
        LocalDateTime currentStart = windowStart;
        while (!currentStart.plus(slotDuration).isAfter(windowEnd)) {
            final LocalDateTime slotStart = currentStart;
            final LocalDateTime slotEnd = currentStart.plus(slotDuration);
            boolean busy = appointments.stream().anyMatch(appointment ->
                    slotStart.isBefore(appointment.getEndDateTime())
                            && slotEnd.isAfter(appointment.getStartDateTime())
            );

            if (!busy) {
                AppointmentAvailabilitySlotResponse slot = new AppointmentAvailabilitySlotResponse();
                slot.setStartDateTime(slotStart);
                slot.setEndDateTime(slotEnd);
                slots.add(slot);
            }

            currentStart = currentStart.plus(slotDuration);
        }

        return slots;
    }
}
