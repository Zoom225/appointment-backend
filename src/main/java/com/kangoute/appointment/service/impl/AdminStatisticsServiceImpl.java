package com.kangoute.appointment.service.impl;

import com.kangoute.appointment.dto.response.AdminStatisticsResponse;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.InvalidStatisticsPeriodException;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final long ACTIVE_WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public AdminStatisticsResponse getStatistics(LocalDateTime periodFrom, LocalDateTime periodTo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime activeSince = now.minusDays(ACTIVE_WINDOW_DAYS);
        LocalDateTime effectiveTo = periodTo != null ? periodTo : now;
        LocalDateTime effectiveFrom = periodFrom != null ? periodFrom : effectiveTo.minusDays(ACTIVE_WINDOW_DAYS);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new InvalidStatisticsPeriodException("Statistics period start must be before end");
        }

        return AdminStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsersLast30Days(appointmentRepository.countDistinctUsersWithAppointmentsBetween(activeSince, now))
                .totalAppointments(appointmentRepository.count())
                .appointmentsInPeriod(appointmentRepository.countAppointmentsBetween(effectiveFrom, effectiveTo))
                .pendingAppointments(appointmentRepository.countByStatus(AppointmentStatus.PENDING))
                .confirmedAppointments(appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED))
                .cancelledAppointments(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED))
                .activeSince(activeSince)
                .periodFrom(effectiveFrom)
                .periodTo(effectiveTo)
                .build();
    }
}
