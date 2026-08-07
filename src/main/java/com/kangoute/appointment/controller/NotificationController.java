package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.mapper.AppointmentNotificationMapper;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.service.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class NotificationController {

    private final AppointmentNotificationService appointmentNotificationService;
    private final AppointmentNotificationMapper appointmentNotificationMapper;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<AppointmentNotificationResponse> getMyNotifications() {
        return appointmentNotificationService.getMyNotifications(currentUserService.getCurrentUserId());
    }

    @PatchMapping("/{id}/read")
    public AppointmentNotificationResponse markAsRead(@PathVariable Long id) {
        return appointmentNotificationMapper.toResponse(
                appointmentNotificationService.markAsRead(id, currentUserService.getCurrentUserId())
        );
    }
}
