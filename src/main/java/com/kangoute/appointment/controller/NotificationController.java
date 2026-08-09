package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.mapper.AppointmentNotificationMapper;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.service.AppointmentNotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final AppointmentNotificationService appointmentNotificationService;
    private final AppointmentNotificationMapper appointmentNotificationMapper;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<AppointmentNotificationResponse> getMyNotifications(
            Pageable pageable,
            @RequestParam(required = false) AppointmentNotificationType type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo
    ) {
        return appointmentNotificationService.getMyNotifications(
                currentUserService.getCurrentUserId(),
                pageable,
                type,
                unreadOnly,
                createdFrom,
                createdTo
        );
    }

    @PatchMapping("/{id}/read")
    public AppointmentNotificationResponse markAsRead(@PathVariable Long id) {
        return appointmentNotificationMapper.toResponse(
                appointmentNotificationService.markAsRead(id, currentUserService.getCurrentUserId())
        );
    }
}
