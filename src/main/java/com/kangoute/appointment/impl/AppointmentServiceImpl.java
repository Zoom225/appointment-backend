package com.kangoute.appointment.impl;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.AppointmentConflictException;
import com.kangoute.appointment.exception.InvalidAppointmentTimeException;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Override
    public Appointment createAppointment(Appointment appointment) {
        if (appointment.getStartDateTime().isAfter(appointment.getEndDateTime())
                || appointment.getStartDateTime().isEqual(appointment.getEndDateTime())) {
            throw new InvalidAppointmentTimeException("Appointment start date must be before end date");
        }

        boolean conflict = appointmentRepository.existsByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                appointment.getUser().getId(),
                appointment.getEndDateTime(),
                appointment.getStartDateTime()
        );

        if (conflict) {
            throw new AppointmentConflictException("User already has an appointment during this time slot");
        }

        if (appointment.getStatus() == null) {
            appointment.setStatus(AppointmentStatus.PENDING);
        }
        return appointmentRepository.save(appointment);
    }

    @Override
    public Appointment updateAppointment(Long id, Appointment appointment) {
        Appointment existingAppointment = getAppointmentById(id);

        if (appointment.getStartDateTime().isAfter(appointment.getEndDateTime())
                || appointment.getStartDateTime().isEqual(appointment.getEndDateTime())) {
            throw new InvalidAppointmentTimeException("Appointment start date must be before end date");
        }

        boolean conflict = appointmentRepository.existsByUserIdAndIdNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                existingAppointment.getUser().getId(),
                existingAppointment.getId(),
                appointment.getEndDateTime(),
                appointment.getStartDateTime()
        );

        if (conflict) {
            throw new AppointmentConflictException("User already has an appointment during this time slot");
        }

        existingAppointment.setStartDateTime(appointment.getStartDateTime());
        existingAppointment.setEndDateTime(appointment.getEndDateTime());
        existingAppointment.setReason(appointment.getReason());

        return appointmentRepository.save(existingAppointment);
    }

    @Override
    public Appointment cancelAppointment(Long id) {
        Appointment existingAppointment = getAppointmentById(id);
        existingAppointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(existingAppointment);
    }

    @Override
    public Appointment updateStatus(Long id, AppointmentStatus status) {
        Appointment existingAppointment = getAppointmentById(id);
        existingAppointment.setStatus(status);
        return appointmentRepository.save(existingAppointment);
    }

    @Override
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    @Override
    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
}
