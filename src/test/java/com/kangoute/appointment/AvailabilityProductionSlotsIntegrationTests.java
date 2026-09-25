package com.kangoute.appointment;

import com.kangoute.appointment.config.AppointmentAvailabilityProperties;
import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.security.JwtService;
import com.kangoute.appointment.service.AppointmentAvailabilityService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:availability-production-slots;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false")
@Transactional
class AvailabilityProductionSlotsIntegrationTests {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);
    @Autowired AppointmentAvailabilityService availability;
    @Autowired AppointmentAvailabilityProperties properties;
    @Autowired AppointmentRepository appointments;
    @Autowired UserRepository userRepository;
    @Autowired UserService users;
    @Autowired JwtService jwt;
    @Autowired WebApplicationContext context;
    @MockitoBean Clock clock;
    private User user;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-09-25T08:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Paris"));
        user = createUser("availability@example.com");
        assertEquals(0, appointments.count());
    }

    @Test
    void exactFutureMondayHasEighteenSlotsWithConfiguredBoundaries() {
        assertEquals(LocalTime.of(9, 0), properties.getWorkdayStart());
        assertEquals(LocalTime.of(18, 0), properties.getWorkdayEnd());
        assertEquals(30, properties.getSlotMinutes());
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), properties.getWorkingDays());
        var slots = availability.getAvailableSlots(user.getId(), DATE);
        assertEquals(18, slots.size());
        for (int i = 0; i < 18; i++) {
            assertEquals(DATE.atTime(9, 0).plusMinutes(i * 30L), slots.get(i).getStartDateTime());
            assertEquals(DATE.atTime(9, 30).plusMinutes(i * 30L), slots.get(i).getEndDateTime());
        }
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PENDING", "CONFIRMED", "SCHEDULED"})
    void oneActiveAppointmentOnlyBlocksItsOwnSlotGlobally(AppointmentStatus status) {
        var other = createUser("other@example.com");
        seed(other, DATE.atTime(10, 0), DATE.atTime(10, 30), status);
        var slots = availability.getAvailableSlots(user.getId(), DATE);
        assertEquals(17, slots.size());
        assertFalse(slots.stream().anyMatch(slot -> slot.getStartDateTime().equals(DATE.atTime(10, 0))));
        assertTrue(slots.stream().anyMatch(slot -> slot.getStartDateTime().equals(DATE.atTime(9, 30))));
        assertTrue(slots.stream().anyMatch(slot -> slot.getStartDateTime().equals(DATE.atTime(10, 30))));
        assertEquals(slots.stream().map(s -> s.getStartDateTime()).toList(),
                availability.getAvailableSlots(other.getId(), DATE).stream().map(s -> s.getStartDateTime()).toList());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "COMPLETED"})
    void terminalAppointmentDoesNotBlockAnySlot(AppointmentStatus status) {
        seed(user, DATE.atTime(10, 0), DATE.atTime(10, 30), status);
        assertEquals(18, availability.getAvailableSlots(user.getId(), DATE).size());
    }

    @Test
    void exactSundayIsClosed() {
        assertTrue(availability.getAvailableSlots(user.getId(), LocalDate.of(2026, 10, 4)).isEmpty());
    }

    @Test
    void authenticatedEndpointWithoutUserIdReturnsExpectedDto() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/api/appointments/availability").param("date", "2026-10-05")
                        .header("Authorization", "Bearer " + jwt.generateToken(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(18))
                .andExpect(jsonPath("$[0].startDateTime").value("2026-10-05T09:00:00"))
                .andExpect(jsonPath("$[0].endDateTime").value("2026-10-05T09:30:00"))
                .andExpect(jsonPath("$[17].startDateTime").value("2026-10-05T17:30:00"))
                .andExpect(jsonPath("$[17].endDateTime").value("2026-10-05T18:00:00"))
                .andExpect(jsonPath("$[0].available").doesNotExist());
        mvc.perform(get("/api/appointments/availability").param("date", "2026-10-05"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void diagnosticQueryIdentifiesOnlyOverlappingActiveDemoAndRealAppointments() {
        var demo = createUser("demo.fixture@example.com");
        demo.setDemoAccountType(DemoAccountType.USER);
        userRepository.saveAndFlush(demo);
        var demoBooking = seed(demo, DATE.atTime(10, 0), DATE.atTime(10, 30), AppointmentStatus.PENDING);
        var realBooking = seed(user, DATE.atTime(11, 0), DATE.atTime(11, 30), AppointmentStatus.CONFIRMED);
        seed(user, DATE.atTime(12, 0), DATE.atTime(12, 30), AppointmentStatus.CANCELLED);
        seed(user, DATE.atTime(13, 0), DATE.atTime(13, 30), AppointmentStatus.COMPLETED);
        seed(user, DATE.atTime(8, 30), DATE.atTime(9, 0), AppointmentStatus.SCHEDULED);
        seed(user, DATE.atTime(18, 0), DATE.atTime(18, 30), AppointmentStatus.PENDING);
        var blockers = appointments.findByStatusInAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                AppointmentStatus.activeStatuses(), DATE.atTime(18, 0), DATE.atTime(9, 0));
        assertEquals(java.util.Set.of(demoBooking.getId(), realBooking.getId()),
                blockers.stream().map(Appointment::getId).collect(java.util.stream.Collectors.toSet()));
        blockers.forEach(a -> System.out.printf("Availability fixture blocker status=%s start=%s end=%s%n",
                a.getStatus(), a.getStartDateTime(), a.getEndDateTime()));
        assertEquals(16, availability.getAvailableSlots(user.getId(), DATE).size());
    }

    @Test
    void historicalActiveIntervalSpanningTheDayCanExplainEmptyCalendar() {
        seed(user, DATE.minusDays(1).atTime(8, 0), DATE.plusDays(1).atTime(18, 0), AppointmentStatus.SCHEDULED);
        assertTrue(availability.getAvailableSlots(user.getId(), DATE).isEmpty());
    }

    @Test
    void clockAtLastSlotStartExplainsEmptyPastCalendar() {
        when(clock.instant()).thenReturn(DATE.atTime(17, 30).atZone(ZoneId.of("Europe/Paris")).toInstant());
        assertTrue(availability.getAvailableSlots(user.getId(), DATE).isEmpty());
    }

    @Test
    void utcAndParisBothGiveEighteenSlotsForThisFutureDate() {
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        assertEquals(18, availability.getAvailableSlots(user.getId(), DATE).size());
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Paris"));
        assertEquals(18, availability.getAvailableSlots(user.getId(), DATE).size());
    }

    private User createUser(String email) {
        return users.createUser(User.builder().firstName("Availability").lastName("Fixture")
                .email(email).password("TestOnly2026!").build());
    }
    private Appointment seed(User owner, LocalDateTime start, LocalDateTime end, AppointmentStatus status) {
        return appointments.saveAndFlush(Appointment.builder().user(owner).startDateTime(start)
                .endDateTime(end).status(status).reason("Availability diagnostic fixture").build());
    }
}
