package com.kangoute.appointment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsResponse {

    private long totalUsers;
    private long activeUsersLast30Days;
    private long totalAppointments;
    private long appointmentsInPeriod;
    private long pendingAppointments;
    private long confirmedAppointments;
    private long cancelledAppointments;
    private LocalDateTime activeSince;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
}
