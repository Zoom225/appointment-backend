package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.response.AdminStatisticsResponse;
import com.kangoute.appointment.service.AdminStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping
    @Operation(summary = "Statistiques du tableau de bord", description = "Totaux, rendez-vous du jour, actifs à venir, terminés et annulés ; calculs en base.")
    public AdminStatisticsResponse getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodTo
    ) {
        return adminStatisticsService.getStatistics(periodFrom, periodTo);
    }
}
