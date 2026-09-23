package com.kangoute.appointment;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.exception.AppointmentConflictException;
import com.kangoute.appointment.repository.*;
import com.kangoute.appointment.security.CustomUserDetails;
import com.kangoute.appointment.service.AppointmentService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:booking-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;LOCK_TIMEOUT=10000")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppointmentConcurrencyIntegrationTests {
    @Autowired AppointmentService service;
    @Autowired UserService users;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentAuditRepository audits;
    @Autowired AppointmentNotificationRepository notifications;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notifications.deleteAll();
            audits.deleteAll();
            appointments.deleteAll();
            userRepository.deleteAll();
        });
    }

    @Test
    void twoUsersRacingForAnEmptySlotProduceOneReservation() throws Exception {
        race(user("first@example.com"), user("second@example.com"), false);
    }

    @Test
    void oneUserRacingForDifferentSlotsStillGetsOnlyOneReservation() throws Exception {
        User user = user("same@example.com");
        race(user, user, true);
    }

    @Test
    void calendarLockIsHeldUntilTransactionCompletes() throws Exception {
        User user = user("locked@example.com");
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            var attempt = new java.util.concurrent.atomic.AtomicReference<Future<Appointment>>();
            CountDownLatch entered = new CountDownLatch(1);
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                appointments.lockBookingCalendar();
                attempt.set(executor.submit(() -> {
                    entered.countDown();
                    return service.createAppointment(booking(user, AppointmentTestDates.nextWorkingDate().atTime(9, 0)));
                }));
                try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> attempt.get().get(300, TimeUnit.MILLISECONDS));
                    assertEquals(0, appointments.count());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            });
            assertNotNull(attempt.get().get(10, TimeUnit.SECONDS).getId());
        }
    }

    private void race(User first, User second, boolean differentSlots) throws Exception {
        LocalDateTime start = AppointmentTestDates.nextWorkingDate().atTime(10, 0);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = executor.submit(() -> reserve(first, start, barrier));
            Future<Boolean> b = executor.submit(() -> reserve(second, differentSlots ? start.plusHours(1) : start, barrier));
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, appointments.count());
        assertEquals(1, audits.count());
        assertEquals(1, notifications.count());
    }

    private boolean reserve(User user, LocalDateTime start, CyclicBarrier barrier) throws Exception {
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            barrier.await(5, TimeUnit.SECONDS);
            service.createAppointment(booking(user, start));
            return true;
        } catch (AppointmentConflictException expected) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Appointment booking(User user, LocalDateTime start) {
        return Appointment.builder().user(user).startDateTime(start).endDateTime(start.plusMinutes(30)).reason("Concurrent booking").build();
    }

    private User user(String email) {
        return users.createUser(User.builder().firstName("Concurrent").lastName("User").email(email).password("test-password").build());
    }
}
