package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AppointmentNotificationRepository extends JpaRepository<AppointmentNotification, Long>, JpaSpecificationExecutor<AppointmentNotification> {

    List<AppointmentNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<AppointmentNotification> findAllByOrderByCreatedAtDesc();

    boolean existsByAppointmentIdAndType(Long appointmentId, AppointmentNotificationType type);
}
