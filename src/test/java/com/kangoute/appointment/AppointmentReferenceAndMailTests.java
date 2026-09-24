package com.kangoute.appointment;

import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.service.AppointmentMailService;
import com.kangoute.appointment.service.AppointmentReferenceService;
import com.kangoute.appointment.service.QrCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentReferenceAndMailTests {
    @Test
    void referencesAndTokensUseIndependentRandomValues() {
        var generator = new AppointmentReferenceService(Clock.fixed(Instant.parse("2030-01-07T08:00:00Z"), ZoneOffset.UTC));
        var references = new HashSet<String>();
        var tokens = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            String reference = generator.generateReference();
            String token = generator.generateVerificationToken();
            assertTrue(reference.matches("RDV-20300107-[A-Za-z0-9_-]{16}"));
            assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
            assertTrue(references.add(reference));
            assertTrue(tokens.add(token));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void disabledMailNeverResolvesSmtpOrGeneratesQr() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        var qr = mock(QrCodeService.class);
        var service = new AppointmentMailService(mock(ApplicationEventPublisher.class), provider, qr);
        service.sendAfterCommit(event());
        verifyNoInteractions(provider, qr);
    }

    @Test
    @SuppressWarnings("unchecked")
    void incompleteMailConfigurationDoesNotPropagateAnException() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        var service = new AppointmentMailService(mock(ApplicationEventPublisher.class), provider, mock(QrCodeService.class));
        ReflectionTestUtils.setField(service, "enabled", true);
        assertDoesNotThrow(() -> service.sendAfterCommit(event()));
    }

    private AppointmentMailService.MailEvent event() {
        return new AppointmentMailService.MailEvent("contact@example.com", "Jean", "RDV-test", "A".repeat(43),
                LocalDateTime.of(2030, 1, 8, 10, 0), "Consultation", AppointmentStatus.PENDING);
    }
}
