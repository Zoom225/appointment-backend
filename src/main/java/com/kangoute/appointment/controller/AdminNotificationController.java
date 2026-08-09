package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import com.kangoute.appointment.service.AppointmentNotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {

    private final AppointmentNotificationService appointmentNotificationService;

    @GetMapping
    public Page<AppointmentNotificationResponse> getAllNotifications(
            Pageable pageable,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) AppointmentNotificationType type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo
    ) {
        return appointmentNotificationService.getAllNotifications(
                pageable,
                recipientId,
                type,
                unreadOnly,
                createdFrom,
                createdTo
        );
    }
}
