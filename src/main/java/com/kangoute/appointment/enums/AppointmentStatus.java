package com.kangoute.appointment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.Locale;

public enum AppointmentStatus {
    PENDING,
    SCHEDULED,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    public static List<AppointmentStatus> activeStatuses() {
        return List.of(PENDING, SCHEDULED, CONFIRMED);
    }

    public boolean isActive() {
        return activeStatuses().contains(this);
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case PENDING -> target == SCHEDULED || target == CONFIRMED || target == CANCELLED;
            case SCHEDULED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

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
