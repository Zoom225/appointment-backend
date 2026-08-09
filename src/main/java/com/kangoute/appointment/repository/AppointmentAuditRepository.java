package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.AppointmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentAuditRepository extends JpaRepository<AppointmentAudit, Long> {

    List<AppointmentAudit> findByAppointmentIdOrderByOccurredAtDesc(Long appointmentId);
}
