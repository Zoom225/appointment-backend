package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.response.AppointmentAvailabilitySlotResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentAvailabilityService {

    void validateAppointmentWindow(LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<AppointmentAvailabilitySlotResponse> getAvailableSlots(Long userId, LocalDate date);
}
