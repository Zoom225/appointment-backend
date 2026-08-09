package com.kangoute.appointment.entity;

import com.kangoute.appointment.enums.AppointmentAuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointment_audits",
        indexes = {
                @Index(name = "idx_audits_appointment_occurred", columnList = "appointment_id,occurredAt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentAuditAction action;

    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;
}
