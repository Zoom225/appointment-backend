package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByUserId(Long userId);

    List<Appointment> findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Long userId,
            LocalDateTime endDateTime,
            LocalDateTime startDateTime
    );

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

    long countByStatus(com.kangoute.appointment.enums.AppointmentStatus status);

    @Query("""
            select count(a)
            from Appointment a
            where a.startDateTime >= :from
              and a.startDateTime <= :to
            """)
    long countAppointmentsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            select count(distinct a.user.id)
            from Appointment a
            where a.startDateTime >= :from
              and a.startDateTime <= :to
            """)
    long countDistinctUsersWithAppointmentsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<Appointment> findByStatusNotAndReminderSentAtIsNullAndStartDateTimeBetween(
            com.kangoute.appointment.enums.AppointmentStatus status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

}
