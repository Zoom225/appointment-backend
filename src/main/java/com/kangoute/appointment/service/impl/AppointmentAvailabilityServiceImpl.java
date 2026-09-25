package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.config.AppointmentAvailabilityProperties;
import com.kangoute.appointment.dto.response.AppointmentAvailabilitySlotResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.AppointmentOutsideAvailabilityException;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AppointmentAvailabilityServiceImpl implements AppointmentAvailabilityService {

    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final AppointmentAvailabilityProperties properties;
    private final Clock clock;

    @Override
    public void validateAppointmentWindow(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (properties.getSlotMinutes() <= 0) {
            throw new AppointmentOutsideAvailabilityException("La duree du creneau doit etre superieure a zero");
        }

        if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
            throw new AppointmentOutsideAvailabilityException("Le rendez-vous doit commencer et se terminer le meme jour");
        }

        if (!properties.getWorkingDays().contains(startDateTime.getDayOfWeek())) {
            throw new AppointmentOutsideAvailabilityException("Le rendez-vous doit etre planifie un jour ouvrable");
        }

        LocalTime startTime = startDateTime.toLocalTime();
        LocalTime endTime = endDateTime.toLocalTime();
        if (startTime.isBefore(properties.getWorkdayStart()) || endTime.isAfter(properties.getWorkdayEnd())) {
            throw new AppointmentOutsideAvailabilityException("Le rendez-vous doit rester dans les horaires de travail");
        }
        Duration duration = Duration.between(startDateTime, endDateTime);
        Duration offset = Duration.between(properties.getWorkdayStart(), startTime);
        long slotSeconds = properties.getSlotMinutes() * 60L;
        if (duration.isNegative() || duration.isZero() || duration.toSeconds() % slotSeconds != 0
                || duration.getNano() != 0 || offset.toSeconds() % slotSeconds != 0 || offset.getNano() != 0) {
            throw new AppointmentOutsideAvailabilityException("Le rendez-vous doit respecter la grille des créneaux de " + properties.getSlotMinutes() + " minutes.");
        }
    }

    @Override
    public List<AppointmentAvailabilitySlotResponse> getAvailableSlots(Long userId, LocalDate date) {
        userService.getUserById(userId);
        if (properties.getSlotMinutes() <= 0) {
            throw new AppointmentOutsideAvailabilityException("La duree du creneau doit etre superieure a zero");
        }

        LocalDateTime windowStart = date.atTime(properties.getWorkdayStart());
        LocalDateTime windowEnd = date.atTime(properties.getWorkdayEnd());
        LocalDateTime now = LocalDateTime.now(clock);
        log.debug("Availability configuration date={} workingDays={} windowStart={} windowEnd={} slotMinutes={} now={} zone={}",
                date, properties.getWorkingDays(), windowStart, windowEnd, properties.getSlotMinutes(), now, clock.getZone());
        if (!properties.getWorkingDays().contains(date.getDayOfWeek())) {
            log.info("Availability date={} windowStart={} windowEnd={} generatedSlots=0 reason=NON_WORKING_DAY",
                    date, windowStart, windowEnd);
            return List.of();
        }

        Duration slotDuration = Duration.ofMinutes(properties.getSlotMinutes());

        List<Appointment> appointments = appointmentRepository
                .findByStatusInAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                        AppointmentStatus.activeStatuses(), windowEnd, windowStart)
                .stream()
                .sorted(Comparator.comparing(Appointment::getStartDateTime))
                .toList();

        if (log.isDebugEnabled()) {
            appointments.forEach(appointment -> log.debug("Availability blocker date={} status={} start={} end={}",
                    date, appointment.getStatus(), appointment.getStartDateTime(), appointment.getEndDateTime()));
        }

        List<AppointmentAvailabilitySlotResponse> slots = new java.util.ArrayList<>();
        LocalDateTime currentStart = windowStart;
        int candidateSlots = 0;
        int busySlots = 0;
        int elapsedSlots = 0;
        while (!currentStart.plus(slotDuration).isAfter(windowEnd)) {
            candidateSlots++;
            final LocalDateTime slotStart = currentStart;
            final LocalDateTime slotEnd = currentStart.plus(slotDuration);
            boolean busy = appointments.stream().anyMatch(appointment ->
                    slotStart.isBefore(appointment.getEndDateTime())
                            && slotEnd.isAfter(appointment.getStartDateTime())
            );

            if (busy) {
                busySlots++;
            } else if (!slotStart.isAfter(now)) {
                elapsedSlots++;
            } else {
                AppointmentAvailabilitySlotResponse slot = new AppointmentAvailabilitySlotResponse();
                slot.setStartDateTime(slotStart);
                slot.setEndDateTime(slotEnd);
                slots.add(slot);
            }

            currentStart = currentStart.plus(slotDuration);
        }

        log.info("Availability date={} windowStart={} windowEnd={} now={} zone={} activeAppointments={} candidateSlots={} busySlots={} elapsedSlots={} generatedSlots={}",
                date, windowStart, windowEnd, now, clock.getZone(), appointments.size(), candidateSlots,
                busySlots, elapsedSlots, slots.size());
        return slots;
    }
}
