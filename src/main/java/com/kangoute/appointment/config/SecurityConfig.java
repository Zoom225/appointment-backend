package com.kangoute.appointment.config;

import com.kangoute.appointment.security.CustomUserDetailsService;
import com.kangoute.appointment.security.JwtAuthenticationFilter;

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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /*
     * ============================
     * SECURITY FILTER CHAIN
     * ============================
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) throws Exception {

        http
                // API REST : CSRF désactivé
                .csrf(AbstractHttpConfigurer::disable)

                // Active la configuration CORS définie plus bas
                .cors(Customizer.withDefaults())

                // JWT => aucune session HTTP côté serveur
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Configuration des autorisations
                .authorizeHttpRequests(auth -> auth

                        /*
                         * AUTHENTIFICATION
                         * Login accessible sans JWT
                         */
                        .requestMatchers("/api/auth/**").permitAll()

                        /*
                         * CRÉATION D'UN UTILISATEUR
                         * Accessible sans être connecté
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users",
                                "/api/users/**"
                        ).permitAll()

                        /*
                         * SWAGGER / OPENAPI
                         */
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs/swagger-config"
                        ).permitAll()

                        /*
                         * IMPORTANT POUR CORS
                         *
                         * Le navigateur envoie d'abord une requête OPTIONS
                         * avant certains appels POST/PUT/PATCH/DELETE.
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * Toutes les autres routes nécessitent
                         * une authentification JWT.
                         */
                        .anyRequest().authenticated()
                );

        /*
         * Provider utilisé pour authentifier
         * email/password.
         */
        http.authenticationProvider(authenticationProvider);

        /*
         * Vérifie le JWT avant le filtre
         * UsernamePasswordAuthenticationFilter.
         */
        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }


    /*
     * ============================
     * AUTHENTICATION PROVIDER
     * ============================
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(customUserDetailsService);

        authenticationProvider.setPasswordEncoder(
                passwordEncoder
        );

        return authenticationProvider;
    }


    /*
     * ============================
     * AUTHENTICATION MANAGER
     * ============================
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }


    /*
     * ============================
     * PASSWORD ENCODER
     * ============================
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    /*
     * ============================
     * CORS
     * ============================
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Frontends autorisés à appeler le backend.
         *
         * localhost = développement Angular
         * Vercel = frontend de production
         */
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:4200",
                        "https://appointment-front-gilt.vercel.app"
                )
        );

        /*
         * Méthodes HTTP autorisées.
         */
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * Headers envoyés par Angular.
         *
         * Authorization est nécessaire pour :
         * Authorization: Bearer <JWT>
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        /*
         * Headers que le navigateur peut lire.
         */
        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        /*
         * Autorise les credentials.
         *
         * Nous utilisons des origines explicites et non "*",
         * donc cette configuration est valide.
         */
        configuration.setAllowCredentials(true);

        /*
         * Durée pendant laquelle le navigateur peut
         * mettre en cache le résultat du preflight OPTIONS.
         */
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        /*
         * Applique CORS à toute l'API.
         */
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
