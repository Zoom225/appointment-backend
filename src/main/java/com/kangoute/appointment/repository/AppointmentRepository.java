package com.kangoute.appointment.repository;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByPublicReference(String publicReference);

    Optional<Appointment> findByVerificationToken(String verificationToken);

    boolean existsByPublicReference(String publicReference);

    @Query(value = "select id from appointment_booking_lock where id = 1 for update", nativeQuery = true)
    Long lockBookingCalendar();

    @Query("""
            select count(a) > 0 from Appointment a
            where a.user.id = :userId and a.status in :statuses and a.startDateTime > :now
            and (:excludedId is null or a.id <> :excludedId)
            """)
    boolean hasFutureActiveAppointment(Long userId, Collection<AppointmentStatus> statuses,
                                       LocalDateTime now, Long excludedId);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.status in :statuses and a.startDateTime < :end and a.endDateTime > :start
            and (:excludedId is null or a.id <> :excludedId)
            """)
    boolean hasOccupiedSlot(Collection<AppointmentStatus> statuses,
                            LocalDateTime start, LocalDateTime end, Long excludedId);

    List<Appointment> findByStatusInAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Collection<AppointmentStatus> statuses,
            LocalDateTime end, LocalDateTime start);

    long countByStartDateTimeGreaterThanEqualAndStartDateTimeLessThan(LocalDateTime from, LocalDateTime to);

    long countByStatusInAndStartDateTimeAfter(
            Collection<AppointmentStatus> statuses, LocalDateTime now);

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

    long countByStatus(AppointmentStatus status);

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
            AppointmentStatus status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

}
