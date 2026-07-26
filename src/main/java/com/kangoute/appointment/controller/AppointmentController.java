package com.kangoute.appointment.controller;

import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.dto.response.AppointmentResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.mapper.AppointmentMapper;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;
    private final AppointmentMapper appointmentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        User user = userService.getUserById(request.getUserId());
        Appointment appointment = appointmentMapper.toEntity(request, user);
        return appointmentMapper.toResponse(appointmentService.createAppointment(appointment));
    }

    @GetMapping("/{id}")
    public AppointmentResponse getAppointmentById(@PathVariable Long id) {
        return appointmentMapper.toResponse(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public List<AppointmentResponse> getAppointments(@RequestParam(required = false) Long userId) {
        List<Appointment> appointments = (userId == null)
                ? appointmentService.getAllAppointments()
                : appointmentService.getAppointmentsByUserId(userId);

        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }
}
