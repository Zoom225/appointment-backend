package com.kangoute.appointment.mapper;

import com.kangoute.appointment.dto.request.AppointmentCreateRequest;
import com.kangoute.appointment.dto.response.AppointmentResponse;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public Appointment toEntity(AppointmentCreateRequest request, User user) {
        Appointment appointment = new Appointment();
        appointment.setStartDateTime(request.getStartDateTime());
        appointment.setEndDateTime(request.getEndDateTime());
        appointment.setReason(request.getReason());
        appointment.setUser(user);
        return appointment;
    }

    public AppointmentResponse toResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setStartDateTime(appointment.getStartDateTime());
        response.setEndDateTime(appointment.getEndDateTime());
        response.setReason(appointment.getReason());
        response.setStatus(appointment.getStatus());
        response.setUserId(appointment.getUser().getId());
        return response;
    }
}
