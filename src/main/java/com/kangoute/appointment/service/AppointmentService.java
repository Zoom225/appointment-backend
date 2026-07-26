package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {

    Appointment createAppointment(Appointment appointment);

    Appointment updateAppointment(Long id, Appointment appointment);

    Appointment cancelAppointment(Long id);

    Appointment updateStatus(Long id, AppointmentStatus status);

    Appointment getAppointmentById(Long id);

    List<Appointment> getAppointmentsByUserId(Long userId);

    List<Appointment> getAllAppointments();
}
