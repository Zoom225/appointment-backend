package com.kangoute.appointment.mapper;

import com.kangoute.appointment.dto.response.AppointmentAuditResponse;
import com.kangoute.appointment.entity.AppointmentAudit;
import org.springframework.stereotype.Component;

@Component
public class AppointmentAuditMapper {

    public AppointmentAuditResponse toResponse(AppointmentAudit audit) {
        AppointmentAuditResponse response = new AppointmentAuditResponse();
        response.setId(audit.getId());
        response.setAppointmentId(audit.getAppointment().getId());
        response.setAction(audit.getAction());
        response.setActorEmail(audit.getActorEmail());
        response.setOccurredAt(audit.getOccurredAt());
        response.setDetails(audit.getDetails());
        return response;
    }
}
