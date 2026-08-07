package com.kangoute.appointment.impl;

import com.kangoute.appointment.service.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentNotificationService appointmentNotificationService;

    @Scheduled(fixedDelayString = "${appointment.notifications.reminder-check-ms:60000}")
    public void checkReminders() {
        appointmentNotificationService.sendDueReminders(LocalDateTime.now());
    }
}
