package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.AppointmentConflictException;
import com.kangoute.appointment.exception.InvalidAppointmentTimeException;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.specification.AppointmentSpecifications;
import com.kangoute.appointment.enums.AppointmentAuditAction;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.service.AppointmentAuditService;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.AppointmentNotificationService;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentAvailabilityService appointmentAvailabilityService;
    private final AppointmentAuditService appointmentAuditService;
    private final AppointmentNotificationService appointmentNotificationService;
    private final CurrentUserService currentUserService;

    @Override
    public Appointment createAppointment(Appointment appointment) {
        if (appointment.getStartDateTime().isAfter(appointment.getEndDateTime())
                || appointment.getStartDateTime().isEqual(appointment.getEndDateTime())) {
            throw new InvalidAppointmentTimeException("Appointment start date must be before end date");
        }

        appointmentAvailabilityService.validateAppointmentWindow(
                appointment.getStartDateTime(),
                appointment.getEndDateTime()
        );

        boolean conflict = appointmentRepository.existsByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                appointment.getUser().getId(),
                appointment.getEndDateTime(),
                appointment.getStartDateTime()
        );

        if (conflict) {
            throw new AppointmentConflictException("User already has an appointment during this time slot");
        }

        if (appointment.getStatus() == null) {
            appointment.setStatus(AppointmentStatus.PENDING);
        }
        Appointment saved = appointmentRepository.save(appointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.CREATED, buildCreatedDetails(saved));
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.CREATED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        return saved;
    }

    @Override
    public Appointment updateAppointment(Long id, Appointment appointment) {
        Appointment existingAppointment = getAppointmentById(id);

        if (appointment.getStartDateTime().isAfter(appointment.getEndDateTime())
                || appointment.getStartDateTime().isEqual(appointment.getEndDateTime())) {
            throw new InvalidAppointmentTimeException("Appointment start date must be before end date");
        }

        appointmentAvailabilityService.validateAppointmentWindow(
                appointment.getStartDateTime(),
                appointment.getEndDateTime()
        );

        boolean conflict = appointmentRepository.existsByUserIdAndIdNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                existingAppointment.getUser().getId(),
                existingAppointment.getId(),
                appointment.getEndDateTime(),
                appointment.getStartDateTime()
        );

        if (conflict) {
            throw new AppointmentConflictException("User already has an appointment during this time slot");
        }

        String before = buildAppointmentSummary(existingAppointment);
        existingAppointment.setStartDateTime(appointment.getStartDateTime());
        existingAppointment.setEndDateTime(appointment.getEndDateTime());
        existingAppointment.setReason(appointment.getReason());
        existingAppointment.setReminderSentAt(null);

        Appointment saved = appointmentRepository.save(existingAppointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.UPDATED, before + " -> " + buildAppointmentSummary(saved));
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.UPDATED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        return saved;
    }

    @Override
    public Appointment cancelAppointment(Long id) {
        Appointment existingAppointment = getAppointmentById(id);
        if (existingAppointment.getStatus() == AppointmentStatus.CANCELLED) {
            return existingAppointment;
        }
        existingAppointment.setStatus(AppointmentStatus.CANCELLED);
        existingAppointment.setReminderSentAt(null);
        Appointment saved = appointmentRepository.save(existingAppointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.CANCELLED, "Status changed to CANCELLED");
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.CANCELLED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        return saved;
    }

    @Override
    public Appointment updateStatus(Long id, AppointmentStatus status) {
        Appointment existingAppointment = getAppointmentById(id);
        if (status == null) {
            throw new InvalidAppointmentTimeException("Appointment status must not be null");
        }
        AppointmentStatus before = existingAppointment.getStatus();
        existingAppointment.setStatus(status);
        existingAppointment.setReminderSentAt(null);
        Appointment saved = appointmentRepository.save(existingAppointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.STATUS_CHANGED, "Status changed from " + before + " to " + status);
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.STATUS_CHANGED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> getAppointmentsByUserId(Long userId, Pageable pageable, AppointmentStatus status, java.time.LocalDateTime startFrom, java.time.LocalDateTime startTo) {
        return appointmentRepository.findAll(
                AppointmentSpecifications.hasUserId(userId)
                        .and(AppointmentSpecifications.hasStatus(status))
                        .and(AppointmentSpecifications.overlaps(startFrom, startTo)),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> getAllAppointments(Pageable pageable, Long userId, AppointmentStatus status, java.time.LocalDateTime startFrom, java.time.LocalDateTime startTo) {
        return appointmentRepository.findAll(
                AppointmentSpecifications.hasUserId(userId)
                        .and(AppointmentSpecifications.hasStatus(status))
                        .and(AppointmentSpecifications.overlaps(startFrom, startTo)),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    private String buildCreatedDetails(Appointment appointment) {
        return "Created " + buildAppointmentSummary(appointment);
    }

    private String buildAppointmentSummary(Appointment appointment) {
        return "appointment["
                + "start=" + appointment.getStartDateTime()
                + ", end=" + appointment.getEndDateTime()
                + ", reason=" + appointment.getReason()
                + ", status=" + appointment.getStatus()
                + "]";
    }
}
