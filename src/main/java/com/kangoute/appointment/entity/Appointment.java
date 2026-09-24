package com.kangoute.appointment.entity;

import com.kangoute.appointment.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointments",
        indexes = {
                @Index(name = "idx_appointments_user_start", columnList = "user_id,startDateTime"),
                @Index(name = "idx_appointments_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private String reason;

    @Column(length = 80)
    private String contactFirstName;

    @Column(length = 80)
    private String contactLastName;

    @Column(length = 254)
    private String contactEmail;

    @Column(unique = true, length = 40)
    private String publicReference;

    @Column(unique = true, length = 64)
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    private LocalDateTime reminderSentAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
