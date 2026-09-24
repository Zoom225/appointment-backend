package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.config.DemoProperties;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import com.kangoute.appointment.mapper.AppointmentNotificationMapper;
import com.kangoute.appointment.repository.AppointmentNotificationRepository;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.specification.AppointmentNotificationSpecifications;
import com.kangoute.appointment.service.AppointmentNotificationService;
import com.kangoute.appointment.security.DemoAdminAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentNotificationServiceImpl implements AppointmentNotificationService {

    private final AppointmentNotificationRepository appointmentNotificationRepository;
    private final AppointmentNotificationMapper appointmentNotificationMapper;
    private final AppointmentRepository appointmentRepository;
    private final DemoAdminAccess demoAdminAccess;
    private final DemoProperties demoProperties;

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
                .title(type == AppointmentNotificationType.STATUS_CHANGED && appointment.getStatus() == AppointmentStatus.CONFIRMED
                        ? "Rendez-vous confirmé" : buildTitle(type))
                .message(buildMessage(type, appointment, actorEmail))
                .createdAt(LocalDateTime.now())
                .build();

        AppointmentNotification saved = appointmentNotificationRepository.save(notification);
        if (type == AppointmentNotificationType.CREATED && demoProperties.isEnabled()
                && appointment.getUser().getDemoAccountType() == DemoAccountType.USER) {
            var admin = demoAdminAccess.demoAdmin();
            appointmentNotificationRepository.save(AppointmentNotification.builder()
                            .appointment(appointment)
                            .recipient(admin)
                            .type(AppointmentNotificationType.CREATED)
                            .title("Nouveau rendez-vous")
                            .message(appointment.getUser().getFirstName() + " " + appointment.getUser().getLastName()
                                    + " a demandé un rendez-vous pour le "
                                    + appointment.getStartDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")) + ".")
                            .createdAt(LocalDateTime.now())
                            .build());
        }
        return saved;
    }

    @Override
    public void sendDueReminders(LocalDateTime now) {
        appointmentRepository.lockBookingCalendar();
        LocalDateTime reminderStart = now.plusMinutes(reminderMinutesBefore);
        LocalDateTime reminderEnd = reminderStart.plusMinutes(1);

        List<Appointment> appointments = appointmentRepository
                .findByStatusNotAndReminderSentAtIsNullAndStartDateTimeBetween(
                        AppointmentStatus.CANCELLED,
                        reminderStart,
                        reminderEnd
                );

        for (Appointment appointment : appointments) {
            if (!appointment.getStatus().isActive()) continue;
            notifyAppointmentEvent(appointment, AppointmentNotificationType.REMINDER, "SYSTEM");
            appointment.setReminderSentAt(now);
            appointmentRepository.save(appointment);
        }
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Page<AppointmentNotificationResponse> getAllNotifications(Pageable pageable, Long recipientId, AppointmentNotificationType type, Boolean unreadOnly, LocalDateTime createdFrom, LocalDateTime createdTo) {
        demoAdminAccess.assertVisibleNotificationRecipient(recipientId);
        Long demoUserId = demoAdminAccess.isDemoAdmin() ? demoAdminAccess.demoUserId() : null;
        return appointmentNotificationRepository.findAll(
                AppointmentNotificationSpecifications.hasRecipientId(recipientId)
                                .and(AppointmentNotificationSpecifications.hasAppointmentUserId(demoUserId))
                                .and(AppointmentNotificationSpecifications.hasType(type))
                                .and(AppointmentNotificationSpecifications.isUnread(unreadOnly))
                                .and(AppointmentNotificationSpecifications.createdFrom(createdFrom))
                                .and(AppointmentNotificationSpecifications.createdTo(createdTo)),
                        normalizePageable(pageable)
                )
                .map(appointmentNotificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentNotificationResponse> getMyNotifications(Long recipientId) {
        return appointmentNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(appointmentNotificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentNotificationResponse> getAllNotifications() {
        if (demoAdminAccess.isDemoAdmin()) {
            return appointmentNotificationRepository.findAll(
                    AppointmentNotificationSpecifications.hasAppointmentUserId(demoAdminAccess.demoUserId()),
                    Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .map(appointmentNotificationMapper::toResponse).toList();
        }
        return appointmentNotificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(appointmentNotificationMapper::toResponse)
                .toList();
    }

    @Override
    public AppointmentNotification markAsRead(Long notificationId, Long recipientId) {
        AppointmentNotification notification = appointmentNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable avec l'identifiant : " + notificationId));

        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new ResourceNotFoundException("Notification introuvable avec l'identifiant : " + notificationId);
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = appointmentNotificationRepository.save(notification);
        }

        return notification;
    }

    private String buildTitle(AppointmentNotificationType type) {
        return switch (type) {
            case CREATED -> "Demande de rendez-vous enregistrée";
            case UPDATED -> "Rendez-vous modifie";
            case CANCELLED -> "Rendez-vous annule";
            case STATUS_CHANGED -> "Statut du rendez-vous modifie";
            case REMINDER -> "Rappel de rendez-vous";
        };
    }

    private String buildMessage(AppointmentNotificationType type, Appointment appointment, String actorEmail) {
        String date = appointment.getStartDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));
        return switch (type) {
            case CREATED -> "Votre rendez-vous du " + date + " est en attente de confirmation.";
            case UPDATED -> "Rendez-vous modifie par " + actorEmail;
            case CANCELLED -> "Votre rendez-vous a été annulé.";
            case STATUS_CHANGED -> appointment.getStatus() == AppointmentStatus.CONFIRMED
                    ? "Votre rendez-vous du " + date + " est confirmé."
                    : "Le statut de votre rendez-vous du " + date + " est " + appointment.getStatus() + ".";
            case REMINDER -> "Rappel pour le rendez-vous commencant a " + appointment.getStartDateTime();
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
