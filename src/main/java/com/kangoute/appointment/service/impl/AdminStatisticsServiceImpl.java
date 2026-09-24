package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.dto.response.AdminStatisticsResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.InvalidStatisticsPeriodException;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.repository.specification.AppointmentSpecifications;
import com.kangoute.appointment.security.DemoAdminAccess;
import com.kangoute.appointment.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final long ACTIVE_WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;
    private final DemoAdminAccess demoAdminAccess;

    @Override
    public AdminStatisticsResponse getStatistics(LocalDateTime periodFrom, LocalDateTime periodTo) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime activeSince = now.minusDays(ACTIVE_WINDOW_DAYS);
        LocalDateTime effectiveTo = periodTo != null ? periodTo : now;
        LocalDateTime effectiveFrom = periodFrom != null ? periodFrom : effectiveTo.minusDays(ACTIVE_WINDOW_DAYS);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new InvalidStatisticsPeriodException("La date de debut de la periode statistique doit etre avant la date de fin");
        }

        if (demoAdminAccess.isDemoAdmin()) {
            return demoStatistics(now, activeSince, effectiveFrom, effectiveTo);
        }

        return AdminStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsersLast30Days(appointmentRepository.countDistinctUsersWithAppointmentsBetween(activeSince, now))
                .totalAppointments(appointmentRepository.count())
                .todayAppointments(appointmentRepository.countByStartDateTimeGreaterThanEqualAndStartDateTimeLessThan(
                        now.toLocalDate().atStartOfDay(), now.toLocalDate().plusDays(1).atStartOfDay()))
                .upcomingAppointments(appointmentRepository.countByStatusInAndStartDateTimeAfter(AppointmentStatus.activeStatuses(), now))
                .completedAppointments(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED))
                .appointmentsInPeriod(appointmentRepository.countAppointmentsBetween(effectiveFrom, effectiveTo))
                .pendingAppointments(
                        appointmentRepository.countByStatus(AppointmentStatus.PENDING)
                                + appointmentRepository.countByStatus(AppointmentStatus.SCHEDULED)
                )
                .confirmedAppointments(appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED))
                .cancelledAppointments(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED))
                .activeSince(activeSince)
                .periodFrom(effectiveFrom)
                .periodTo(effectiveTo)
                .build();
    }

    private AdminStatisticsResponse demoStatistics(LocalDateTime now, LocalDateTime activeSince,
                                                   LocalDateTime from, LocalDateTime to) {
        Long userId = demoAdminAccess.demoUserId();
        Specification<Appointment> scope = AppointmentSpecifications.hasUserId(userId);
        long pending = count(scope.and(AppointmentSpecifications.hasStatus(AppointmentStatus.PENDING)))
                + count(scope.and(AppointmentSpecifications.hasStatus(AppointmentStatus.SCHEDULED)));
        long recent = count(scope.and(startedBetween(activeSince, now)));
        return AdminStatisticsResponse.builder()
                .totalUsers(demoAdminAccess.visibleUserIds().size())
                .activeUsersLast30Days(recent > 0 ? 1 : 0)
                .totalAppointments(count(scope))
                .todayAppointments(count(scope.and(startedBetween(
                        now.toLocalDate().atStartOfDay(), now.toLocalDate().plusDays(1).atStartOfDay()))))
                .upcomingAppointments(count(scope.and(AppointmentSpecifications.upcoming(now))))
                .completedAppointments(count(scope.and(AppointmentSpecifications.hasStatus(AppointmentStatus.COMPLETED))))
                .appointmentsInPeriod(count(scope.and(startedBetween(from, to))))
                .pendingAppointments(pending)
                .confirmedAppointments(count(scope.and(AppointmentSpecifications.hasStatus(AppointmentStatus.CONFIRMED))))
                .cancelledAppointments(count(scope.and(AppointmentSpecifications.hasStatus(AppointmentStatus.CANCELLED))))
                .activeSince(activeSince)
                .periodFrom(from)
                .periodTo(to)
                .build();
    }

    private long count(Specification<Appointment> specification) {
        return appointmentRepository.count(specification);
    }

    private Specification<Appointment> startedBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("startDateTime"), from),
                cb.lessThanOrEqualTo(root.get("startDateTime"), to));
    }
}
