package com.kangoute.appointment.service;

import com.kangoute.appointment.dto.response.AdminStatisticsResponse;

import java.time.LocalDateTime;

public interface AdminStatisticsService {

    AdminStatisticsResponse getStatistics(LocalDateTime periodFrom, LocalDateTime periodTo);
}
