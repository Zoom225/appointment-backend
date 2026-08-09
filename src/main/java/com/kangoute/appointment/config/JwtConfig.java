package com.kangoute.appointment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.jwt.secret:VGhpcy1kZWZhdWx0LXNlY3JldC1tdXN0LWJlLXN1YnN0aXR1dGVkLWF0LXByb2R1Y3Rpb24=}") String secret) {
        return NimbusJwtEncoder.withSecretKey(
                new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")
        ).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret:VGhpcy1kZWZhdWx0LXNlY3JldC1tdXN0LWJlLXN1YnN0aXR1dGVkLWF0LXByb2R1Y3Rpb24=}") String secret) {
        return NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")
        ).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
