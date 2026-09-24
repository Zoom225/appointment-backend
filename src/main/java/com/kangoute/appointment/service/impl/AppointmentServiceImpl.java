package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentAuditAction;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.AppointmentConflictException;
import com.kangoute.appointment.exception.InvalidAppointmentTimeException;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import com.kangoute.appointment.mapper.AppointmentMapper;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.specification.AppointmentSpecifications;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.security.DemoAdminAccess;
import com.kangoute.appointment.service.AppointmentAuditService;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.AppointmentNotificationService;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.AppointmentReferenceService;
import com.kangoute.appointment.service.AppointmentMailService;
import com.kangoute.appointment.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentAvailabilityService appointmentAvailabilityService;
    private final AppointmentAuditService appointmentAuditService;
    private final AppointmentNotificationService appointmentNotificationService;
    private final CurrentUserService currentUserService;
    private final DemoAdminAccess demoAdminAccess;
    private final UserService userService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentReferenceService referenceService;
    private final AppointmentMailService mailService;
    private final Clock clock;
    private final EntityManager entityManager;

    @Override
    public Appointment bookAppointment(AppointmentCreateRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        if (request.getUserId() != null) {
            if (!currentUserService.isAdmin() && !request.getUserId().equals(userId)) {
                throw new AccessDeniedException("Acces refuse");
            }
            if (currentUserService.isAdmin()) userId = request.getUserId();
        }
        demoAdminAccess.assertVisibleUser(userId);
        if (demoAdminAccess.isDemoAdmin()) demoAdminAccess.assertDemoAppointmentOwner(userId);
        return createAppointment(appointmentMapper.toEntity(request, userService.getUserById(userId)));
    }

    @Override
    public Appointment createAppointment(Appointment appointment) {
        lockCalendar();
        assertAccess(appointment);
        validateReservation(appointment, null);
        normalizeContact(appointment);
        if (appointment.getPublicReference() == null) {
            String reference;
            do { reference = referenceService.generateReference(); }
            while (appointmentRepository.existsByPublicReference(reference));
            appointment.setPublicReference(reference);
        }
        if (appointment.getVerificationToken() == null) {
            appointment.setVerificationToken(referenceService.generateVerificationToken());
        }
        appointment.setStatus(AppointmentStatus.PENDING);
        Appointment saved = appointmentRepository.saveAndFlush(appointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.CREATED, buildCreatedDetails(saved));
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.CREATED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        mailService.schedule(saved);
        return saved;
    }

    @Override
    public Appointment updateAppointment(Long id, Appointment appointment) {
        Appointment existingAppointment = lockedAppointment(id);
        if (!existingAppointment.getStatus().isActive()) {
            throw new AppointmentConflictException("Un rendez-vous terminé ou annulé ne peut plus être modifié.");
        }
        appointment.setUser(existingAppointment.getUser());
        validateReservation(appointment, id);

        String before = buildAppointmentSummary(existingAppointment);
        existingAppointment.setStartDateTime(appointment.getStartDateTime());
        existingAppointment.setEndDateTime(appointment.getEndDateTime());
        existingAppointment.setReason(appointment.getReason());
        existingAppointment.setReminderSentAt(null);

        Appointment saved = appointmentRepository.saveAndFlush(existingAppointment);
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
        Appointment existingAppointment = lockedAppointment(id);
        validateTransition(existingAppointment, AppointmentStatus.CANCELLED);
        existingAppointment.setStatus(AppointmentStatus.CANCELLED);
        existingAppointment.setReminderSentAt(null);
        Appointment saved = appointmentRepository.saveAndFlush(existingAppointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.CANCELLED, "Statut change en ANNULE");
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.CANCELLED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        mailService.schedule(saved);
        return saved;
    }

    @Override
    public Appointment updateStatus(Long id, AppointmentStatus status) {
        if (status == AppointmentStatus.CANCELLED) return cancelAppointment(id);
        if (!currentUserService.isAdmin()) throw new AccessDeniedException("Acces refuse");
        Appointment existingAppointment = lockedAppointment(id);
        if (status == null) {
            throw new InvalidAppointmentTimeException("Le statut du rendez-vous ne doit pas etre nul");
        }
        AppointmentStatus before = existingAppointment.getStatus();
        validateTransition(existingAppointment, status);
        if (status.isActive()) validateReservation(existingAppointment, id);
        existingAppointment.setStatus(status);
        existingAppointment.setReminderSentAt(null);
        Appointment saved = appointmentRepository.saveAndFlush(existingAppointment);
        appointmentAuditService.record(saved, AppointmentAuditAction.STATUS_CHANGED, "Statut change de " + before + " a " + status);
        appointmentNotificationService.notifyAppointmentEvent(
                saved,
                AppointmentNotificationType.STATUS_CHANGED,
                currentUserService.getCurrentUserEmailOrSystem()
        );
        mailService.schedule(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable avec l'identifiant : " + id));
        assertAccess(appointment);
        return appointment;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> getAppointmentsByUserId(Long userId, Pageable pageable, AppointmentStatus status, LocalDateTime startFrom, LocalDateTime startTo) {
        userId = authorizedUserId(userId);
        return appointmentRepository.findAll(
                AppointmentSpecifications.hasUserId(userId)
                        .and(AppointmentSpecifications.hasStatus(status))
                        .and(AppointmentSpecifications.overlaps(startFrom, startTo)),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> getAllAppointments(Pageable pageable, Long userId, AppointmentStatus status, LocalDateTime startFrom, LocalDateTime startTo) {
        return searchAppointments(pageable, userId, status, startFrom, startTo, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> searchAppointments(Pageable pageable, Long userId, AppointmentStatus status,
            LocalDateTime startFrom, LocalDateTime startTo, String query) {
        userId = authorizedUserId(userId);
        return appointmentRepository.findAll(
                AppointmentSpecifications.hasUserId(userId)
                        .and(AppointmentSpecifications.hasStatus(status))
                        .and(AppointmentSpecifications.matchesUser(query))
                        .and(AppointmentSpecifications.overlaps(startFrom, startTo)),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(authorizedUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        Long userId = authorizedUserId(null);
        return userId == null ? appointmentRepository.findAll() : appointmentRepository.findByUserId(userId);
    }

    private String buildCreatedDetails(Appointment appointment) {
        return "Cree " + buildAppointmentSummary(appointment);
    }

    private void normalizeContact(Appointment appointment) {
        if (appointment.getContactFirstName() == null) appointment.setContactFirstName(appointment.getUser().getFirstName());
        if (appointment.getContactLastName() == null) appointment.setContactLastName(appointment.getUser().getLastName());
        if (appointment.getContactEmail() == null) appointment.setContactEmail(appointment.getUser().getEmail());
        appointment.setContactFirstName(appointment.getContactFirstName().trim());
        appointment.setContactLastName(appointment.getContactLastName().trim());
        appointment.setContactEmail(appointment.getContactEmail().trim().toLowerCase(Locale.ROOT));
        if (appointment.getContactFirstName().length() < 2 || appointment.getContactFirstName().length() > 80
                || appointment.getContactLastName().length() < 2 || appointment.getContactLastName().length() > 80
                || appointment.getContactEmail().length() > 254
                || !appointment.getContactEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new InvalidAppointmentTimeException("Coordonnees de contact invalides");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> getMyAppointments(Pageable pageable, boolean upcoming) {
        var selection = AppointmentSpecifications.upcoming(LocalDateTime.now(clock));
        if (!upcoming) selection = Specification.not(selection);
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(upcoming ? Sort.Direction.ASC : Sort.Direction.DESC, "startDateTime").and(Sort.by("id")));
        }
        return appointmentRepository.findAll(selection.and(
                AppointmentSpecifications.hasUserId(currentUserService.getCurrentUserId())), pageable);
    }

    private void lockCalendar() {
        if (appointmentRepository.lockBookingCalendar() == null) {
            throw new IllegalStateException("Booking lock is missing; apply database migrations");
        }
    }

    private Appointment lockedAppointment(Long id) {
        lockCalendar();
        Appointment appointment = getAppointmentById(id);
        entityManager.refresh(appointment);
        assertAccess(appointment);
        return appointment;
    }

    private void assertAccess(Appointment appointment) {
        // Internal jobs may run as SYSTEM; HTTP entry points require authentication.
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) demoAdminAccess.assertDemoAppointmentOwner(appointment.getUser().getId());
        if (authentication != null && !currentUserService.isAdmin()
                && !currentUserService.isCurrentUser(appointment.getUser().getId())) {
            throw new AccessDeniedException("Acces refuse");
        }
    }

    private Long authorizedUserId(Long requestedUserId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return requestedUserId;
        if (currentUserService.isAdmin()) return demoAdminAccess.restrictedUserId(requestedUserId);
        Long currentUserId = currentUserService.getCurrentUserId();
        if (requestedUserId != null && !requestedUserId.equals(currentUserId)) {
            throw new AccessDeniedException("Acces refuse");
        }
        return currentUserId;
    }

    private void validateReservation(Appointment appointment, Long excludedId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (appointment.getStartDateTime() == null || !appointment.getStartDateTime().isAfter(now)) {
            throw new InvalidAppointmentTimeException("La date du rendez-vous doit être dans le futur.");
        }
        if (appointment.getEndDateTime() == null || !appointment.getEndDateTime().isAfter(appointment.getStartDateTime())) {
            throw new InvalidAppointmentTimeException("L'heure de debut du rendez-vous doit etre avant l'heure de fin");
        }
        appointmentAvailabilityService.validateAppointmentWindow(appointment.getStartDateTime(), appointment.getEndDateTime());
        if (appointmentRepository.hasFutureActiveAppointment(appointment.getUser().getId(),
                AppointmentStatus.activeStatuses(), now, excludedId)) {
            throw new AppointmentConflictException("Vous avez déjà un rendez-vous actif. Annulez-le ou attendez sa finalisation avant d'en réserver un nouveau.");
        }
        if (appointmentRepository.hasOccupiedSlot(AppointmentStatus.activeStatuses(),
                appointment.getStartDateTime(), appointment.getEndDateTime(), excludedId)) {
            throw new AppointmentConflictException("Ce créneau n'est plus disponible.");
        }
    }

    private void validateTransition(Appointment appointment, AppointmentStatus target) {
        if (!appointment.getStatus().canTransitionTo(target)) {
            throw new AppointmentConflictException("Transition de statut non autorisée : " + appointment.getStatus() + " -> " + target);
        }
    }

    private String buildAppointmentSummary(Appointment appointment) {
        return "rendezvous["
                + "start=" + appointment.getStartDateTime()
                + ", end=" + appointment.getEndDateTime()
                + ", reason=" + appointment.getReason()
                + ", status=" + appointment.getStatus()
                + "]";
    }
}
