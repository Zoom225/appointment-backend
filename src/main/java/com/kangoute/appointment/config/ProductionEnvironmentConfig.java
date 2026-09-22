package com.kangoute.appointment.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionEnvironmentConfig {

    // Validate before datasource/JWT bean creation. Never include values in errors.
    @Bean
    static BeanFactoryPostProcessor validateProductionEnvironment(Environment environment) {
        return beanFactory -> {
            if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                throw new IllegalStateException("Profiles dev and prod must not be active together");
            }
            for (String variable : List.of("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME",
                    "SPRING_DATASOURCE_PASSWORD", "JWT_SECRET")) {
                requireVariable(environment, variable);
            }
            if (!environment.getRequiredProperty("spring.datasource.url").startsWith("jdbc:postgresql:")) {
                throw new IllegalStateException("Production requires a PostgreSQL SPRING_DATASOURCE_URL");
            }
            byte[] key;
            try {
                key = Base64.getDecoder().decode(environment.getRequiredProperty("JWT_SECRET"));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("JWT_SECRET must be Base64 encoded");
            }
            if (key.length < 32) {
                throw new IllegalStateException("JWT_SECRET must contain at least 32 decoded bytes");
            }
            if (environment.getProperty("app.demo.enabled", Boolean.class, false)) {
                requireVariable(environment, "APP_DEMO_PASSWORD");
            }
        };
    }

    private static void requireVariable(Environment environment, String variable) {
        String value = environment.getProperty(variable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required production environment variable: " + variable);
        }
    }
}
