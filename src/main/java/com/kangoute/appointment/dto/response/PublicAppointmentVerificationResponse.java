package com.kangoute.appointment.dto.response;

import com.kangoute.appointment.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record PublicAppointmentVerificationResponse(
        String publicReference,
        String contactFirstName,
        String contactLastName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String reason,
        AppointmentStatus status
) { }
