package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.AppointmentNotificationType;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentNotificationService {

    AppointmentNotification notifyAppointmentEvent(Appointment appointment, AppointmentNotificationType type, String actorEmail);

    void sendDueReminders(LocalDateTime now);

    List<AppointmentNotificationResponse> getMyNotifications(Long recipientId);

    List<AppointmentNotificationResponse> getAllNotifications();

    AppointmentNotification markAsRead(Long notificationId, Long recipientId);
}
