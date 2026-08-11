package com.kangoute.appointment.config;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.AppointmentStatus;
import com.kangoute.appointment.repository.AppointmentRepository;
import com.kangoute.appointment.repository.UserRepository;
import com.kangoute.appointment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoProperties demoProperties;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User demoUser = userRepository.findByEmail(demoProperties.getEmail())
                .orElseGet(this::createDemoUser);

        if (!appointmentRepository.findByUserId(demoUser.getId()).isEmpty()) {
            return;
        }

        appointmentRepository.saveAll(buildDemoAppointments(demoUser));
    }

    private User createDemoUser() {
        User demoUser = new User();
        demoUser.setFirstName(demoProperties.getFirstName());
        demoUser.setLastName(demoProperties.getLastName());
        demoUser.setEmail(demoProperties.getEmail());
        demoUser.setPassword(demoProperties.getPassword());
        return userService.createUser(demoUser);
    }

    private List<Appointment> buildDemoAppointments(User demoUser) {
        LocalDateTime base = LocalDateTime.now().withSecond(0).withNano(0);

        return List.of(
                buildAppointment(demoUser, base.plusDays(2).withHour(10).withMinute(0),
                        base.plusDays(2).withHour(10).withMinute(30),
                        "Consultation de demonstration",
                        AppointmentStatus.SCHEDULED),
                buildAppointment(demoUser, base.plusDays(3).withHour(11).withMinute(0),
                        base.plusDays(3).withHour(11).withMinute(30),
                        "Reunion de suivi",
                        AppointmentStatus.CONFIRMED),
                buildAppointment(demoUser, base.minusDays(2).withHour(12).withMinute(0),
                        base.minusDays(2).withHour(12).withMinute(30),
                        "Entretien termine",
                        AppointmentStatus.COMPLETED),
                buildAppointment(demoUser, base.minusDays(3).withHour(13).withMinute(0),
                        base.minusDays(3).withHour(13).withMinute(30),
                        "Rendez-vous annule",
                        AppointmentStatus.CANCELLED),
                buildAppointment(demoUser, base.plusDays(4).withHour(14).withMinute(0),
                        base.plusDays(4).withHour(14).withMinute(30),
                        "Nouveau rendez-vous",
                        AppointmentStatus.PENDING)
        );
    }

    private Appointment buildAppointment(
            User demoUser,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String reason,
            AppointmentStatus status
    ) {
        return Appointment.builder()
                .user(demoUser)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .reason(reason)
                .status(status)
                .build();
    }
}
