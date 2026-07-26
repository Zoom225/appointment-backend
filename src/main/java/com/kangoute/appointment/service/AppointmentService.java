package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.Appointment;

import java.util.List;

public interface AppointmentService {

    Appointment createAppointment(Appointment appointment);

    Appointment getAppointmentById(Long id);

    List<Appointment> getAppointmentsByUserId(Long userId);

    List<Appointment> getAllAppointments();
}
