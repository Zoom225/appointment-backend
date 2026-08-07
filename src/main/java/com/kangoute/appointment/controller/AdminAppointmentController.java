package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.request.AppointmentStatusUpdateRequest;
import com.kangoute.appointment.dto.response.AppointmentAuditResponse;
import com.kangoute.appointment.dto.response.AppointmentResponse;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.mapper.AppointmentMapper;
import com.kangoute.appointment.service.AppointmentAuditService;
import com.kangoute.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentAuditService appointmentAuditService;

    @GetMapping
    public Page<AppointmentResponse> getAllAppointments(
            Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) LocalDateTime startFrom,
            @RequestParam(required = false) LocalDateTime startTo
    ) {
        return appointmentService.getAllAppointments(pageable, userId, status, startFrom, startTo)
                .map(appointmentMapper::toResponse);
    }

    @GetMapping("/{id}")
    public AppointmentResponse getAppointmentById(@PathVariable Long id) {
        return appointmentMapper.toResponse(appointmentService.getAppointmentById(id));
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request
    ) {
        return appointmentMapper.toResponse(appointmentService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/{id}/history")
    public List<AppointmentAuditResponse> getHistory(@PathVariable Long id) {
        return appointmentAuditService.getHistory(id);
    }
}
