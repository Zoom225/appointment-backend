package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    Appointment bookAppointment(AppointmentCreateRequest request);

    Page<Appointment> getMyAppointments(Pageable pageable, boolean upcoming);

    Page<Appointment> searchAppointments(Pageable pageable, Long userId, AppointmentStatus status,
            LocalDateTime startFrom, LocalDateTime startTo, String query);

    Appointment createAppointment(Appointment appointment);

    Appointment updateAppointment(Long id, Appointment appointment);

    Appointment cancelAppointment(Long id);

    Appointment updateStatus(Long id, AppointmentStatus status);

    Appointment getAppointmentById(Long id);

    Page<Appointment> getAppointmentsByUserId(Long userId, Pageable pageable, AppointmentStatus status, LocalDateTime startFrom, LocalDateTime startTo);

    Page<Appointment> getAllAppointments(Pageable pageable, Long userId, AppointmentStatus status, LocalDateTime startFrom, LocalDateTime startTo);

    List<Appointment> getAppointmentsByUserId(Long userId);

    List<Appointment> getAllAppointments();
}
