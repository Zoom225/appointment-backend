package com.kangoute.appointment.mapper;

import com.kangoute.appointment.dto.request.UserCreateRequest;
import com.kangoute.appointment.dto.request.UserAdminUpdateRequest;
import com.kangoute.appointment.dto.response.UserResponse;
import com.kangoute.appointment.entity.Role;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.service.RoleService;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        return user;
    }

    public User toEntity(UserAdminUpdateRequest request, RoleService roleService) {
        User user = new User();
        updateEntity(request, user, roleService);
        return user;
    }

    public void updateEntity(UserAdminUpdateRequest request, User user, RoleService roleService) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRoles(request.getRoles().stream()
                .map(roleService::createRole)
                .collect(Collectors.toSet()));
    }

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return response;
    }
}
