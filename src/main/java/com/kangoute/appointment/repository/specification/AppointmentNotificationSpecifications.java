package com.kangoute.appointment.repository.specification;

import com.kangoute.appointment.entity.AppointmentNotification;
import com.kangoute.appointment.enums.AppointmentNotificationType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

public final class AppointmentNotificationSpecifications {

    private AppointmentNotificationSpecifications() {
    }

    public static Specification<AppointmentNotification> hasRecipientIds(Set<Long> recipientIds) {
        return (root, query, cb) -> recipientIds == null ? cb.conjunction()
                : root.get("recipient").get("id").in(recipientIds);
    }

    public static Specification<AppointmentNotification> hasRecipientId(Long recipientId) {
        return (root, query, cb) -> {
            if (recipientId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("recipient").get("id"), recipientId);
        };
    }

    public static Specification<AppointmentNotification> hasAppointmentUserId(Long userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("appointment").get("user").get("id"), userId);
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
