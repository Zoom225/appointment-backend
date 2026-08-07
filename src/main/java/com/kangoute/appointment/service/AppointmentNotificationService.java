package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentNotificationService {

    AppointmentNotification notifyAppointmentEvent(Appointment appointment, AppointmentNotificationType type, String actorEmail);

    void sendDueReminders(LocalDateTime now);

    Page<AppointmentNotificationResponse> getMyNotifications(Long recipientId, Pageable pageable, AppointmentNotificationType type, Boolean unreadOnly, LocalDateTime createdFrom, LocalDateTime createdTo);

    Page<AppointmentNotificationResponse> getAllNotifications(Pageable pageable, Long recipientId, AppointmentNotificationType type, Boolean unreadOnly, LocalDateTime createdFrom, LocalDateTime createdTo);

    List<AppointmentNotificationResponse> getMyNotifications(Long recipientId);

    List<AppointmentNotificationResponse> getAllNotifications();

    AppointmentNotification markAsRead(Long notificationId, Long recipientId);
}
