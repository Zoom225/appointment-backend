package com.kangoute.appointment;

import com.kangoute.appointment.config.CorsProperties;
import com.kangoute.appointment.config.JwtConfig;
import com.kangoute.appointment.config.ProductionEnvironmentConfig;
import com.kangoute.appointment.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentProfilesTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                // Read the real main profile files without inheriting workstation credentials
                // or the application.properties that intentionally shadows them in tests.
                context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                new ConfigDataApplicationContextInitializer().initialize(context);
            })
            .withPropertyValues("spring.config.location=file:./src/main/resources/")
            .withUserConfiguration(ProductionEnvironmentConfig.class, JwtConfig.class);

    @Test
    void devUsesH2AndOnlyLocalCorsWithoutExternalVariables() {
        runner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
            CorsProperties cors = Binder.get(context.getEnvironment()).bind("app.cors", CorsProperties.class).get();
            assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:4200", "http://127.0.0.1:4200");
        });
    }

    @Test
    void productionUsesRuntimeVariablesAndNoImplicitCorsOrigins() {
        production(variables()).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
            assertThat(context.getEnvironment().getProperty("app.jwt.secret")).isEqualTo(variables().get("JWT_SECRET"));
            CorsProperties properties = Binder.get(context.getEnvironment()).bind("app.cors", CorsProperties.class).get();
            var cors = new SecurityConfig().corsConfigurationSource(properties)
                    .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/users"));
            assertThat(cors.getAllowedOrigins()).isEmpty();
            assertThat(cors.checkOrigin("http://localhost:4200")).isNull();
            assertThat(cors.checkOrigin("https://other.vercel.app")).isNull();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"FRONTEND_URL", "APP_FRONTEND_URL", "CORS_ALLOWED_ORIGINS"})
    void productionAcceptsExplicitFrontendVariables(String variable) {
        production(variables()).withPropertyValues(variable + "=https://configured.vercel.app").run(context -> {
            assertThat(context).hasNotFailed();
            CorsProperties properties = Binder.get(context.getEnvironment()).bind("app.cors", CorsProperties.class).get();
            var cors = new SecurityConfig().corsConfigurationSource(properties)
                    .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/users"));
            assertThat(cors.checkOrigin("https://configured.vercel.app")).isEqualTo("https://configured.vercel.app");
            assertThat(cors.checkOrigin("http://localhost:4200")).isNull();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD", "JWT_SECRET"})
    void productionRejectsMissingRequiredVariables(String variable) {
        Map<String, String> variables = variables();
        variables.remove(variable);
        production(variables).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining(variable);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD", "JWT_SECRET"})
    void productionRejectsBlankRequiredVariables(String variable) {
        Map<String, String> variables = variables();
        variables.put(variable, "");
        production(variables).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining(variable);
        });
    }

    @Test
    void productionRejectsH2EvenWhenExplicitlyConfigured() {
        Map<String, String> variables = variables();
        variables.put("SPRING_DATASOURCE_URL", "jdbc:h2:mem:forbidden");
        production(variables).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("PostgreSQL");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-base64!", "c2hvcnQ="})
    void productionRejectsInvalidKeysWithoutExposingTheirValues(String secret) {
        Map<String, String> variables = variables();
        variables.put("JWT_SECRET", secret);
        production(variables).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("JWT_SECRET");
            assertThat(context.getStartupFailure().getMessage()).doesNotContain(secret);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev,prod", "prod,dev"})
    void devAndProdCannotBeCombined(String profiles) {
        production(variables()).withPropertyValues("spring.profiles.active=" + profiles).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("must not be active together");
        });
    }

    @Test
    void noProfileDoesNotSupplyAKnownJwtKey() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void productionDemoRequiresExplicitPassword() {
        production(variables()).withPropertyValues("APP_DEMO_ENABLED=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("APP_DEMO_PASSWORD");
        });
    }

    private ApplicationContextRunner production(Map<String, String> variables) {
        return runner.withPropertyValues("spring.profiles.active=prod")
                .withPropertyValues(variables.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new));
    }

    private Map<String, String> variables() {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/configuration_test");
        variables.put("SPRING_DATASOURCE_USERNAME", "configuration_test");
        variables.put("SPRING_DATASOURCE_PASSWORD", "test-only-not-a-real-password");
        variables.put("JWT_SECRET", Base64.getEncoder().encodeToString(
                "configuration-test-only-public-key".getBytes(StandardCharsets.UTF_8)));
        return variables;
    }
}
