package com.kangoute.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "appointment.availability")
public class AppointmentAvailabilityProperties {

    private LocalTime workdayStart = LocalTime.of(9, 0);
    private LocalTime workdayEnd = LocalTime.of(18, 0);
    private int slotMinutes = 30;
    private List<DayOfWeek> workingDays = List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
    );

    public LocalTime getWorkdayStart() {
        return workdayStart;
    }

    public void setWorkdayStart(LocalTime workdayStart) {
        this.workdayStart = workdayStart;
    }

    public LocalTime getWorkdayEnd() {
        return workdayEnd;
    }

    public void setWorkdayEnd(LocalTime workdayEnd) {
        this.workdayEnd = workdayEnd;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public List<DayOfWeek> getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(List<DayOfWeek> workingDays) {
        this.workingDays = workingDays;
    }
}
