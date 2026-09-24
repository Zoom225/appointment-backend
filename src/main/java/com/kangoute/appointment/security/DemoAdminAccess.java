package com.kangoute.appointment.security;

import com.kangoute.appointment.config.DemoProperties;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.List;

/** Limits the public admin identity to the configured demo accounts. */
@Component
@RequiredArgsConstructor
public class DemoAdminAccess {
    private final DemoProperties demoProperties;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public boolean isDemoAdmin() {
        return demoProperties.isEnabled()
                && SecurityContextHolder.getContext().getAuthentication() != null
                && currentUserService.isAdmin()
                && currentUserService.getCurrentUserDetails().getUser().getDemoAccountType() == DemoAccountType.ADMIN;
    }

    public Long demoUserId() {
        return demoAccount(DemoAccountType.USER).getId();
    }

    public User demoAdmin() {
        return demoAccount(DemoAccountType.ADMIN);
    }

    private User demoAccount(DemoAccountType type) {
        List<User> accounts = userRepository.findByDemoAccountType(type);
        if (accounts.size() != 1) {
            throw new AccessDeniedException("Compte de demonstration indisponible");
        }
        return accounts.getFirst();
    }

    public void assertDemoAppointmentOwner(Long ownerId) {
        if (isDemoAdmin() && !demoUserId().equals(ownerId)) {
            throw new AccessDeniedException("Acces refuse");
        }
    }

    public Long restrictedUserId(Long requestedUserId) {
        if (!isDemoAdmin()) return requestedUserId;
        Long demoId = demoUserId();
        if (requestedUserId != null && !requestedUserId.equals(demoId)) {
            throw new AccessDeniedException("Acces refuse");
        }
        return demoId;
    }

    public Set<Long> visibleUserIds() {
        Long demoId = demoUserId();
        return Set.of(demoId, currentUserService.getCurrentUserId());
    }

    public void assertVisibleNotificationRecipient(Long recipientId) {
        if (isDemoAdmin() && recipientId != null && !visibleUserIds().contains(recipientId)) {
            throw new AccessDeniedException("Acces refuse");
        }
    }

    public void assertVisibleUser(Long userId) {
        if (isDemoAdmin() && !visibleUserIds().contains(userId)) {
            throw new AccessDeniedException("Acces refuse");
        }
    }

    public void denyUserMutation() {
        if (isDemoAdmin()) throw new AccessDeniedException("Acces refuse");
    }
}
