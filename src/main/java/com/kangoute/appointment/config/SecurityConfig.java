package com.kangoute.appointment.config;

import com.kangoute.appointment.security.CustomUserDetailsService;
import com.kangoute.appointment.security.JwtAuthenticationFilter;
import com.kangoute.appointment.security.ApiSecurityErrorHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider,
            ApiSecurityErrorHandler securityErrorHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/appointments/verify").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs/swagger-config"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(customUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(resolveAllowedOrigins(corsProperties));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> resolveAllowedOrigins(CorsProperties corsProperties) {
        Set<String> allowedOrigins = new LinkedHashSet<>();
        if (corsProperties.getAllowedOrigins() != null) {
            corsProperties.getAllowedOrigins().forEach(origin -> addAllowedOrigin(allowedOrigins, origin));
        }
        addAllowedOrigin(allowedOrigins, corsProperties.getFrontendUrl());

        return List.copyOf(allowedOrigins);
    }

    private void addAllowedOrigin(Set<String> allowedOrigins, String origin) {
        if (origin == null || origin.isBlank()) return;
        String normalized = origin.trim();
        if (normalized.contains("*")) {
            throw new IllegalArgumentException("CORS origins must be explicit URLs without wildcards");
        }
        allowedOrigins.add(normalized);
    }
}
