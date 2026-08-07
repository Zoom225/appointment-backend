package com.kangoute.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriseDeRendezVousApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriseDeRendezVousApplication.class, args);
    }

}
