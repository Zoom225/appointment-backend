package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.response.PublicAppointmentVerificationResponse;
import com.kangoute.appointment.service.AppointmentVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/appointments")
@RequiredArgsConstructor
public class PublicAppointmentVerificationController {
    private final AppointmentVerificationService verificationService;

    @GetMapping("/verify")
    @Operation(summary = "Vérifier un rendez-vous avec son lien QR")
    public ResponseEntity<PublicAppointmentVerificationResponse> verify(@RequestParam String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .body(verificationService.verify(token));
    }
}
