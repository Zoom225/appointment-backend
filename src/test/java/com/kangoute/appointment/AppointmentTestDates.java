package com.kangoute.appointment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

final class AppointmentTestDates {
    private AppointmentTestDates() { }

    static LocalDate nextWorkingDate() {
        return LocalDate.now().plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }
}
