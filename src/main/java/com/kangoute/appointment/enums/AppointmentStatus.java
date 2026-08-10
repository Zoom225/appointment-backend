package com.kangoute.appointment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum AppointmentStatus {
    PENDING,
    SCHEDULED,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AppointmentStatus fromJson(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (AppointmentStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Statut de rendez-vous invalide : " + value);
    }
}
