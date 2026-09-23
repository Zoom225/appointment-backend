package com.kangoute.appointment;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.exception.AppointmentConflictException;
import com.kangoute.appointment.repository.AppointmentAuditRepository;
import com.kangoute.appointment.repository.AppointmentNotificationRepository;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostgreSqlBookingIntegrationTests {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired AppointmentService service;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentAuditRepository audits;
    @Autowired AppointmentNotificationRepository notifications;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notifications.deleteAll();
            audits.deleteAll();
            appointments.deleteAll();
            userRepository.deleteAll();
        });
    }

    @Test
    void flywayCreatesBookingLockBeforeReservations() {
        var lock = new TransactionTemplate(transactionManager).execute(status -> appointments.lockBookingCalendar());
        assertEquals(1L, lock);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bookingLockBlocksUntilCommitOrRollback(boolean rollback) throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var attempted = new CountDownLatch(1);
            @SuppressWarnings("unchecked")
            Future<Long>[] waiter = new Future[1];
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertEquals(1L, appointments.lockBookingCalendar());
                waiter[0] = executor.submit(() -> {
                    attempted.countDown();
                    return new TransactionTemplate(transactionManager)
                            .execute(inner -> appointments.lockBookingCalendar());
                });
                try {
                    assertTrue(attempted.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> waiter[0].get(300, TimeUnit.MILLISECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                if (rollback) status.setRollbackOnly();
            });
            assertEquals(1L, waiter[0].get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void twoUsersRacingForSameSlotCreateOneAppointment() throws Exception {
        race(user("pg-first@example.com"), user("pg-second@example.com"), false);
    }

    @Test
    void oneUserRacingForDifferentSlotsGetsOnlyOneActiveAppointment() throws Exception {
        User owner = user("pg-same@example.com");
        race(owner, owner, true);
        assertEquals(1, appointments.findByUserId(owner.getId()).size());
    }

    @ParameterizedTest
    @EnumSource(AppointmentStatus.class)
    void activeStatusesBlockAndTerminalStatusesReleaseBooking(AppointmentStatus status) {
        User owner = user("pg-status@example.com");
        LocalDateTime firstSlot = AppointmentTestDates.nextWorkingDate().atTime(10, 0);
        appointments.saveAndFlush(Appointment.builder().user(owner).status(status)
                .startDateTime(firstSlot).endDateTime(firstSlot.plusMinutes(30))
                .reason("Existing").build());
        authenticate(owner);
        Appointment next = booking(owner, firstSlot.plusHours(1));
        if (status.isActive()) {
            assertThrows(AppointmentConflictException.class, () -> service.createAppointment(next));
            assertEquals(1, appointments.findByUserId(owner.getId()).size());
        } else {
            assertNotNull(service.createAppointment(next).getId());
            assertEquals(2, appointments.findByUserId(owner.getId()).size());
        }
    }

    @Test
    void newAndUpdatedAppointmentHaveDatabaseTimestamps() {
        User owner = user("pg-timestamps@example.com");
        authenticate(owner);
        LocalDateTime start = AppointmentTestDates.nextWorkingDate().atTime(10, 0);
        Appointment created = service.createAppointment(booking(owner, start));
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        Appointment updated = service.updateAppointment(created.getId(), booking(owner, start.plusHours(1)));
        assertNotNull(updated.getUpdatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(created.getUpdatedAt()));
        assertEquals(created.getCreatedAt(), updated.getCreatedAt());
    }

    private void race(User first, User second, boolean differentSlots) throws Exception {
        LocalDateTime start = AppointmentTestDates.nextWorkingDate().atTime(11, 0);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = executor.submit(() -> reserve(first, start, barrier));
            Future<Boolean> b = executor.submit(() -> reserve(second, differentSlots ? start.plusHours(1) : start, barrier));
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, appointments.count());
        assertEquals(1, audits.count());
        assertEquals(1, notifications.count());
    }

    private boolean reserve(User owner, LocalDateTime start, CyclicBarrier barrier) throws Exception {
        authenticate(owner);
        try {
            barrier.await(5, TimeUnit.SECONDS);
            service.createAppointment(booking(owner, start));
            return true;
        } catch (AppointmentConflictException expected) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(User owner) {
        CustomUserDetails principal = new CustomUserDetails(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Appointment booking(User owner, LocalDateTime start) {
        return Appointment.builder().user(owner).startDateTime(start)
                .endDateTime(start.plusMinutes(30)).reason("PostgreSQL booking").build();
    }

    private User user(String email) {
        return users.createUser(User.builder().firstName("PostgreSQL").lastName("User")
                .email(email).password(java.util.UUID.randomUUID().toString()).build());
    }
}
