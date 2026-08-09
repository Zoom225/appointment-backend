package com.kangoute.appointment.dto.response;

import com.kangoute.appointment.enums.AppointmentAuditAction;

import java.time.LocalDateTime;

public class AppointmentAuditResponse {

    private Long id;
    private Long appointmentId;
    private AppointmentAuditAction action;
    private String actorEmail;
    private LocalDateTime occurredAt;
    private String details;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public AppointmentAuditAction getAction() {
        return action;
    }

    public void setAction(AppointmentAuditAction action) {
        this.action = action;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
