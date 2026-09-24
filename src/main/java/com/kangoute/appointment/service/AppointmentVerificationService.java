package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.response.PublicAppointmentVerificationResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import com.kangoute.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentVerificationService {
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public PublicAppointmentVerificationResponse verify(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new ResourceNotFoundException("Rendez-vous introuvable");
        }
        Appointment appointment = appointmentRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));
        return new PublicAppointmentVerificationResponse(
                appointment.getPublicReference(), appointment.getContactFirstName(),
                appointment.getContactLastName(), appointment.getStartDateTime(),
                appointment.getEndDateTime(), appointment.getReason(), appointment.getStatus());
    }
}
