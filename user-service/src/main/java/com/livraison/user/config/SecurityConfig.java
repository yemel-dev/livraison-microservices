package com.livraison.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration Spring Security.
 *
 * POURQUOI permitAll() ?
 * Architecture Zero Trust : le Gateway valide les tokens JWT avant
 * de router vers ce service. Ce service fait confiance aux headers
 * X-User-Id et X-User-Role injectés par le Gateway — il n'a pas
 * besoin de re-valider les tokens lui-même.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)      // Désactivé — API REST stateless
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()               // Gateway gère le Zero Trust
            );
        return http.build();
    }

    /**
     * BCrypt pour hacher les mots de passe.
     * Utilisé dans UserService.register() et UserService.login().
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}