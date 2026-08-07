package com.kangoute.appointment.exception;

public class AppointmentOutsideAvailabilityException extends RuntimeException {

    public AppointmentOutsideAvailabilityException(String message) {
        super(message);
    }
}
