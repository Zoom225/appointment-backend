package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.dto.request.AppointmentStatusUpdateRequest;
import com.kangoute.appointment.dto.request.AppointmentUpdateRequest;
import com.kangoute.appointment.dto.response.AppointmentAvailabilitySlotResponse;
import com.kangoute.appointment.dto.response.AppointmentResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.mapper.AppointmentMapper;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentAvailabilityService appointmentAvailabilityService;
    private final AppointmentMapper appointmentMapper;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Réserver un rendez-vous en attente de confirmation", description = "L'utilisateur est déterminé par le JWT. Un seul rendez-vous actif futur est autorisé.")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse createAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        return appointmentMapper.toResponse(appointmentService.bookAppointment(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse getAppointmentById(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        return appointmentMapper.toResponse(appointment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request
    ) {
        Appointment changes = new Appointment();
        appointmentMapper.updateEntity(request, changes);
        return appointmentMapper.toResponse(appointmentService.updateAppointment(id, changes));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Changer le statut (ADMIN) ou annuler son rendez-vous (USER)")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request
    ) {
        return appointmentMapper.toResponse(
                appointmentService.updateStatus(id, request.getStatus())
        );
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Annuler son rendez-vous actif")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse cancelAppointment(@PathVariable Long id) {
        return appointmentMapper.toResponse(appointmentService.cancelAppointment(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<AppointmentResponse> getAppointments(
            Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) LocalDateTime startFrom,
            @RequestParam(required = false) LocalDateTime startTo
    ) {
        Long targetUserId = userId;
        if (!currentUserService.isAdmin()) {
            Long currentUserId = currentUserService.getCurrentUserId();
            if (targetUserId != null && !targetUserId.equals(currentUserId)) {
                throw new AccessDeniedException("Vous ne pouvez acceder qu'a vos propres rendez-vous");
            }
            targetUserId = currentUserId;
        }

        return appointmentService.getAllAppointments(pageable, targetUserId, status, startFrom, startTo)
                .map(appointmentMapper::toResponse);
    }

    @GetMapping("/availability")
    @Operation(summary = "Consulter les créneaux futurs disponibles dans le calendrier partagé")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public java.util.List<AppointmentAvailabilitySlotResponse> getAvailability(
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (userId == null) userId = currentUserService.getCurrentUserId();
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(userId)) {
            throw new AccessDeniedException("Vous ne pouvez acceder qu'a vos propres disponibilites");
        }
        return appointmentAvailabilityService.getAvailableSlots(userId, date);
    }

    @GetMapping("/me/upcoming")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Mes rendez-vous actifs à venir", description = "Ordre chronologique par défaut ; size=1 donne le prochain rendez-vous.")
    public Page<AppointmentResponse> getMyUpcomingAppointments(Pageable pageable) {
        return appointmentService.getMyAppointments(pageable, true).map(appointmentMapper::toResponse);
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Mon historique", description = "Rendez-vous passés, annulés ou terminés, paginés du plus récent au plus ancien.")
    public Page<AppointmentResponse> getMyHistory(Pageable pageable) {
        return appointmentService.getMyAppointments(pageable, false).map(appointmentMapper::toResponse);
    }
}
