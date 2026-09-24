package com.kangoute.appointment.dto.response;

import com.kangoute.appointment.enums.AppointmentStatus;

import java.time.LocalDateTime;

public class AppointmentResponse {

    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String reason;
    private AppointmentStatus status;
    private Long userId;

    private String publicReference;
    private String contactFirstName;
    private String contactLastName;
    private String contactEmail;

    public String getPublicReference() { return publicReference; }
    public void setPublicReference(String publicReference) { this.publicReference = publicReference; }
    public String getContactFirstName() { return contactFirstName; }
    public void setContactFirstName(String contactFirstName) { this.contactFirstName = contactFirstName; }
    public String getContactLastName() { return contactLastName; }
    public void setContactLastName(String contactLastName) { this.contactLastName = contactLastName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
