package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.dto.request.AppointmentUpdateRequest;
import com.kangoute.appointment.dto.response.AppointmentAvailabilitySlotResponse;
import com.kangoute.appointment.dto.response.AppointmentResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.mapper.AppointmentMapper;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private final UserService userService;
    private final AppointmentMapper appointmentMapper;
    private final CurrentUserService currentUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse createAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(request.getUserId())) {
            throw new AccessDeniedException("Vous ne pouvez creer que des rendez-vous pour votre propre compte utilisateur");
        }
        User user = userService.getUserById(request.getUserId());
        Appointment appointment = appointmentMapper.toEntity(request, user);
        return appointmentMapper.toResponse(appointmentService.createAppointment(appointment));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse getAppointmentById(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(appointment.getUser().getId())) {
            throw new AccessDeniedException("Vous ne pouvez acceder qu'a vos propres rendez-vous");
        }
        return appointmentMapper.toResponse(appointment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request
    ) {
        Appointment existingAppointment = appointmentService.getAppointmentById(id);
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(existingAppointment.getUser().getId())) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres rendez-vous");
        }
        Appointment appointment = appointmentMapper.toEntity(request);
        return appointmentMapper.toResponse(appointmentService.updateAppointment(id, appointment));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public AppointmentResponse cancelAppointment(@PathVariable Long id) {
        Appointment existingAppointment = appointmentService.getAppointmentById(id);
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(existingAppointment.getUser().getId())) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres rendez-vous");
        }
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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public java.util.List<AppointmentAvailabilitySlotResponse> getAvailability(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(userId)) {
            throw new AccessDeniedException("Vous ne pouvez acceder qu'a vos propres disponibilites");
        }
        return appointmentAvailabilityService.getAvailableSlots(userId, date);
    }
}
