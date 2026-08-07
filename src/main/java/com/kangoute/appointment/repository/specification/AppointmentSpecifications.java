package com.kangoute.appointment.repository.specification;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AppointmentSpecifications {

    private AppointmentSpecifications() {
    }

    public static Specification<Appointment> hasUserId(Long userId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Appointment> hasStatus(AppointmentStatus status) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Appointment> overlaps(LocalDateTime startFrom, LocalDateTime startTo) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (startFrom == null && startTo == null) {
                return criteriaBuilder.conjunction();
            }

            if (startFrom != null && startTo != null) {
                return criteriaBuilder.and(
                        criteriaBuilder.lessThanOrEqualTo(root.get("startDateTime"), startTo),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDateTime"), startFrom)
                );
            }

            if (startFrom != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("endDateTime"), startFrom);
            }

            return criteriaBuilder.lessThanOrEqualTo(root.get("startDateTime"), startTo);
        };
    }
}
