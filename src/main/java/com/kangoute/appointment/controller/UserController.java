package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.request.UserCreateRequest;
import com.kangoute.appointment.dto.response.UserResponse;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.mapper.UserMapper;
import com.kangoute.appointment.security.CurrentUserService;
import com.kangoute.appointment.service.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userMapper.toEntity(request);
        return userMapper.toResponse(userService.createUser(user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public UserResponse getUserByEmail(@RequestParam String email) {
        if (!currentUserService.isAdmin() && !currentUserService.getCurrentUserEmail().equals(email)) {
            throw new AccessDeniedException("You can only access your own profile");
        }
        return userMapper.toResponse(userService.getUserByEmail(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (!currentUserService.isAdmin() && !currentUserService.isCurrentUser(id)) {
            throw new AccessDeniedException("You can only access your own profile");
        }
        return userMapper.toResponse(user);
    }
}
