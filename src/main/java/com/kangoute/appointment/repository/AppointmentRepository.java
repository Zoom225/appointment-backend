package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserId(Long userId);

    boolean existsByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Long userId,
            LocalDateTime endDateTime,
            LocalDateTime startDateTime
    );

    boolean existsByUserIdAndIdNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Long userId,
            Long id,
            LocalDateTime endDateTime,
            LocalDateTime startDateTime
    );
}
