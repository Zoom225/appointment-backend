package com.kangoute.appointment;

import com.kangoute.appointment.config.CorsProperties;
import com.kangoute.appointment.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigurationTests {
    private CorsConfiguration configuration(CorsProperties properties) {
        return new SecurityConfig().corsConfigurationSource(properties)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/users"));
    }

    @Test
    void defaultsAllowOnlyLocalDevelopmentOrigins() {
        CorsConfiguration cors = configuration(new CorsProperties());
        assertEquals(List.of("http://localhost:4200", "http://127.0.0.1:4200"), cors.getAllowedOrigins());
        assertNull(cors.checkOrigin("https://arbitrary.vercel.app"));
        assertNull(cors.checkOrigin("http://localhost:4300"));
        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), cors.getAllowedMethods());
        assertEquals(false, cors.getAllowCredentials());
    }

    @Test
    void productionFrontendDoesNotImplicitlyAllowLocalhostOrOtherProjects() {
        CorsProperties properties = new CorsProperties();
        properties.setFrontendUrl("https://my-project.vercel.app");
        CorsConfiguration cors = configuration(properties);
        assertEquals(properties.getFrontendUrl(), cors.checkOrigin(properties.getFrontendUrl()));
        assertNull(cors.checkOrigin("http://localhost:4200"));
        assertNull(cors.checkOrigin("https://other-project.vercel.app"));
    }

    @Test
    void wildcardConfigurationIsRejected() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://*.vercel.app"));
        assertThrows(IllegalArgumentException.class, () -> configuration(properties));
    }
}
