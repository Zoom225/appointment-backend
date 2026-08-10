package com.kangoute.appointment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangoute.appointment.dto.request.AppointmentStatusUpdateRequest;
import com.kangoute.appointment.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentStatusJacksonTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void appointmentStatusAcceptsFrontendValuesCaseInsensitively() throws Exception {
        assertEquals(AppointmentStatus.SCHEDULED, readStatus("{\"status\":\"scheduled\"}"));
        assertEquals(AppointmentStatus.CONFIRMED, readStatus("{\"status\":\"confirmed\"}"));
        assertEquals(AppointmentStatus.CANCELLED, readStatus("{\"status\":\"cancelled\"}"));
        assertEquals(AppointmentStatus.COMPLETED, readStatus("{\"status\":\"completed\"}"));
        assertEquals(AppointmentStatus.COMPLETED, readStatus("{\"status\":\"COMPLETED\"}"));
        assertEquals(AppointmentStatus.COMPLETED, readStatus("{\"status\":\"Completed\"}"));
    }

    @Test
    void appointmentStatusRejectsUnknownValue() {
        assertThrows(JsonProcessingException.class, () -> readStatus("{\"status\":\"unknown\"}"));
    }

    private AppointmentStatus readStatus(String json) throws Exception {
        return objectMapper.readValue(json, AppointmentStatusUpdateRequest.class).getStatus();
    }
}
