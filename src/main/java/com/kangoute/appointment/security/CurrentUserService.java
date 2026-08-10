package com.kangoute.appointment.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    public CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AccessDeniedException("Acces refuse");
        }
        return userDetails;
    }

    public Long getCurrentUserId() {
        return getCurrentUserDetails().getUser().getId();
    }

    public String getCurrentUserEmail() {
        return getCurrentUserDetails().getUsername();
    }

    public String getCurrentUserEmailOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "SYSTEM";
        }
        return userDetails.getUsername();
    }

    public boolean isAdmin() {
        return getCurrentUserDetails().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isCurrentUser(Long userId) {
        return getCurrentUserId().equals(userId);
    }
}
