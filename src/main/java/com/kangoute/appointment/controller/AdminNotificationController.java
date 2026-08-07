package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.mapper.AppointmentNotificationMapper;
import com.kangoute.appointment.service.AppointmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AppointmentNotificationService appointmentNotificationService;

    @GetMapping
    public List<AppointmentNotificationResponse> getAllNotifications() {
        return appointmentNotificationService.getAllNotifications();
    }
}
