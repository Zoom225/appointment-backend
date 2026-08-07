package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AppointmentService {

    Appointment createAppointment(Appointment appointment);

    Appointment updateAppointment(Long id, Appointment appointment);

    Appointment cancelAppointment(Long id);

    Appointment updateStatus(Long id, AppointmentStatus status);

    Appointment getAppointmentById(Long id);

    Page<Appointment> getAppointmentsByUserId(Long userId, Pageable pageable, AppointmentStatus status, java.time.LocalDateTime startFrom, java.time.LocalDateTime startTo);

    Page<Appointment> getAllAppointments(Pageable pageable, Long userId, AppointmentStatus status, java.time.LocalDateTime startFrom, java.time.LocalDateTime startTo);

    List<Appointment> getAppointmentsByUserId(Long userId);

    List<Appointment> getAllAppointments();
}
