package com.kangoute.appointment.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class AppointmentReferenceService {
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public AppointmentReferenceService(Clock clock) { this.clock = clock; }

    public String generateReference() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return "RDV-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateVerificationToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
