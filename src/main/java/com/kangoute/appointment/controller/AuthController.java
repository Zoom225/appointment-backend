package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.request.AuthRequest;
import com.kangoute.appointment.dto.response.AuthResponse;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.mapper.AuthMapper;
import com.kangoute.appointment.security.JwtService;
import com.kangoute.appointment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuthMapper authMapper;
    private final JwtService jwtService;

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody AuthRequest request
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        User user = userService.getUserByEmail(request.getEmail());
        return authMapper.toResponse(user, jwtService.generateToken(user), "Login successful");
    }
}
