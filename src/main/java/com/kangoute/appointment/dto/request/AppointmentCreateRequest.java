package com.kangoute.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Locale;

public class AppointmentCreateRequest {

    @NotBlank
    @Size(min = 2, max = 80)
    private String contactFirstName;

    @NotBlank
    @Size(min = 2, max = 80)
    private String contactLastName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String contactEmail;

    @NotNull
    private LocalDateTime startDateTime;

    @NotNull
    private LocalDateTime endDateTime;

    @NotBlank
    private String reason;

    @Schema(description = "Facultatif. USER : identité issue du JWT ; ADMIN : réservation pour cet utilisateur.")
    private Long userId;

    public String getContactFirstName() { return contactFirstName; }
    public void setContactFirstName(String contactFirstName) { this.contactFirstName = contactFirstName == null ? null : contactFirstName.trim(); }
    public String getContactLastName() { return contactLastName; }
    public void setContactLastName(String contactLastName) { this.contactLastName = contactLastName == null ? null : contactLastName.trim(); }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail == null ? null : contactEmail.trim().toLowerCase(Locale.ROOT); }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
