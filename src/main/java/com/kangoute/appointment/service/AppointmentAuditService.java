package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.response.AppointmentAuditResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentAuditAction;

import java.util.List;

public interface AppointmentAuditService {

    void record(Appointment appointment, AppointmentAuditAction action, String details);

    List<AppointmentAuditResponse> getHistory(Long appointmentId);
}
