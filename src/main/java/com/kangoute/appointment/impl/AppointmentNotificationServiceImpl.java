package com.kangoute.appointment.impl;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import com.kangoute.appointment.mapper.AppointmentNotificationMapper;
import com.kangoute.appointment.repository.AppointmentNotificationRepository;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.specification.AppointmentNotificationSpecifications;
import com.kangoute.appointment.service.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentNotificationServiceImpl implements AppointmentNotificationService {

    private final AppointmentNotificationRepository appointmentNotificationRepository;
    private final AppointmentNotificationMapper appointmentNotificationMapper;
    private final AppointmentRepository appointmentRepository;

    @Value("${appointment.notifications.reminder-minutes-before:60}")
    private int reminderMinutesBefore;

    @Override
    public AppointmentNotification notifyAppointmentEvent(Appointment appointment, AppointmentNotificationType type, String actorEmail) {
        if (appointmentNotificationRepository.existsByAppointmentIdAndType(appointment.getId(), type)
                && type == AppointmentNotificationType.REMINDER) {
            return appointmentNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(appointment.getUser().getId())
                    .stream()
                    .filter(notification -> notification.getAppointment().getId().equals(appointment.getId())
                            && notification.getType() == type)
                    .findFirst()
                    .orElse(null);
        }

        AppointmentNotification notification = AppointmentNotification.builder()
                .appointment(appointment)
                .recipient(appointment.getUser())
                .type(type)
                .title(buildTitle(type))
                .message(buildMessage(type, appointment, actorEmail))
                .createdAt(LocalDateTime.now())
                .build();

        return appointmentNotificationRepository.save(notification);
    }

    @Override
    public void sendDueReminders(LocalDateTime now) {
        LocalDateTime reminderStart = now.plusMinutes(reminderMinutesBefore);
        LocalDateTime reminderEnd = reminderStart.plusMinutes(1);

        List<Appointment> appointments = appointmentRepository
                .findByStatusNotAndReminderSentAtIsNullAndStartDateTimeBetween(
                        AppointmentStatus.CANCELLED,
                        reminderStart,
                        reminderEnd
                );

        for (Appointment appointment : appointments) {
            notifyAppointmentEvent(appointment, AppointmentNotificationType.REMINDER, "SYSTEM");
            appointment.setReminderSentAt(now);
            appointmentRepository.save(appointment);
        }
    }

    @Override
    public Page<AppointmentNotificationResponse> getMyNotifications(Long recipientId, Pageable pageable, AppointmentNotificationType type, Boolean unreadOnly, LocalDateTime createdFrom, LocalDateTime createdTo) {
        return appointmentNotificationRepository.findAll(
                        AppointmentNotificationSpecifications.hasRecipientId(recipientId)
                                .and(AppointmentNotificationSpecifications.hasType(type))
                                .and(AppointmentNotificationSpecifications.isUnread(unreadOnly))
                                .and(AppointmentNotificationSpecifications.createdFrom(createdFrom))
                                .and(AppointmentNotificationSpecifications.createdTo(createdTo)),
                        normalizePageable(pageable)
                )
                .map(appointmentNotificationMapper::toResponse);
    }

    @Override
    public Page<AppointmentNotificationResponse> getAllNotifications(Pageable pageable, Long recipientId, AppointmentNotificationType type, Boolean unreadOnly, LocalDateTime createdFrom, LocalDateTime createdTo) {
        return appointmentNotificationRepository.findAll(
                        AppointmentNotificationSpecifications.hasRecipientId(recipientId)
                                .and(AppointmentNotificationSpecifications.hasType(type))
                                .and(AppointmentNotificationSpecifications.isUnread(unreadOnly))
                                .and(AppointmentNotificationSpecifications.createdFrom(createdFrom))
                                .and(AppointmentNotificationSpecifications.createdTo(createdTo)),
                        normalizePageable(pageable)
                )
                .map(appointmentNotificationMapper::toResponse);
    }

    @Override
    public List<AppointmentNotificationResponse> getMyNotifications(Long recipientId) {
        return appointmentNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(appointmentNotificationMapper::toResponse)
                .toList();
    }

    @Override
    public List<AppointmentNotificationResponse> getAllNotifications() {
        return appointmentNotificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(appointmentNotificationMapper::toResponse)
                .toList();
    }

    @Override
    public AppointmentNotification markAsRead(Long notificationId, Long recipientId) {
        AppointmentNotification notification = appointmentNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = appointmentNotificationRepository.save(notification);
        }

        return notification;
    }

    private String buildTitle(AppointmentNotificationType type) {
        return switch (type) {
            case CREATED -> "Appointment created";
            case UPDATED -> "Appointment updated";
            case CANCELLED -> "Appointment cancelled";
            case STATUS_CHANGED -> "Appointment status changed";
            case REMINDER -> "Appointment reminder";
        };
    }

    private String buildMessage(AppointmentNotificationType type, Appointment appointment, String actorEmail) {
        return switch (type) {
            case CREATED -> "Appointment created by " + actorEmail;
            case UPDATED -> "Appointment updated by " + actorEmail;
            case CANCELLED -> "Appointment cancelled by " + actorEmail;
            case STATUS_CHANGED -> "Appointment status changed by " + actorEmail;
            case REMINDER -> "Reminder for appointment starting at " + appointment.getStartDateTime();
        };
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        return pageable;
    }
}
