package com.kangoute.appointment.repository.specification;

import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AppointmentNotificationSpecifications {

    private AppointmentNotificationSpecifications() {
    }

    public static Specification<AppointmentNotification> hasRecipientId(Long recipientId) {
        return (root, query, cb) -> {
            if (recipientId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("recipient").get("id"), recipientId);
        };
    }

    public static Specification<AppointmentNotification> hasType(AppointmentNotificationType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<AppointmentNotification> isUnread(Boolean unreadOnly) {
        return (root, query, cb) -> {
            if (unreadOnly == null || !unreadOnly) {
                return cb.conjunction();
            }
            return cb.isNull(root.get("readAt"));
        };
    }

    public static Specification<AppointmentNotification> createdFrom(LocalDateTime createdFrom) {
        return (root, query, cb) -> {
            if (createdFrom == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    public static Specification<AppointmentNotification> createdTo(LocalDateTime createdTo) {
        return (root, query, cb) -> {
            if (createdTo == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }
}
