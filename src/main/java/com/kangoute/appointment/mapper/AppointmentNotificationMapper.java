package com.kangoute.appointment.mapper;

import com.kangoute.appointment.dto.response.AppointmentNotificationResponse;
import com.kangoute.appointment.entity.AppointmentNotification;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationMapper {

    public AppointmentNotificationResponse toResponse(AppointmentNotification notification) {
        AppointmentNotificationResponse response = new AppointmentNotificationResponse();
        response.setId(notification.getId());
        response.setAppointmentId(notification.getAppointment().getId());
        response.setRecipientId(notification.getRecipient().getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());
        return response;
    }
}
