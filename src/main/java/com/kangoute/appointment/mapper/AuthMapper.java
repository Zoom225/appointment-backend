package com.kangoute.appointment.mapper;

import com.kangoute.appointment.dto.response.AuthResponse;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AuthMapper {

    public AuthResponse toResponse(User user, String token, String message) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()));
        response.setToken(token);
        response.setMessage(message);
        return response;
    }
}
