package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.dto.response.AppointmentAuditResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.AppointmentAudit;
import com.kangoute.appointment.enums.AppointmentAuditAction;
import com.kangoute.appointment.mapper.AppointmentAuditMapper;
import com.kangoute.appointment.repository.AppointmentAuditRepository;
import com.kangoute.appointment.service.AppointmentAuditService;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.security.DemoAdminAccess;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentAuditServiceImpl implements AppointmentAuditService {

    private final AppointmentAuditRepository appointmentAuditRepository;
    private final AppointmentAuditMapper appointmentAuditMapper;
    private final CurrentUserService currentUserService;
    private final DemoAdminAccess demoAdminAccess;
    private final AppointmentRepository appointmentRepository;

    @Override
    public void record(Appointment appointment, AppointmentAuditAction action, String details) {
        AppointmentAudit audit = AppointmentAudit.builder()
                .appointment(appointment)
                .action(action)
                .actorEmail(currentUserService.getCurrentUserEmailOrSystem())
                .occurredAt(LocalDateTime.now())
                .details(details)
                .build();
        appointmentAuditRepository.save(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentAuditResponse> getHistory(Long appointmentId) {
        if (demoAdminAccess.isDemoAdmin()) {
            Long ownerId = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"))
                    .getUser().getId();
            demoAdminAccess.assertDemoAppointmentOwner(ownerId);
        }
        return appointmentAuditRepository.findByAppointmentIdOrderByOccurredAtDesc(appointmentId)
                .stream()
                .map(appointmentAuditMapper::toResponse)
                .toList();
    }
}
