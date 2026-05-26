package com.livraison.colis.config;

import com.livraison.colis.security.HeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security — Zero Trust finale.
 *
 * Remplace la SecurityConfig temporaire de l'étape précédente.
 *
 * Principes appliqués :
 * 1. Stateless   → aucune session HTTP côté serveur
 * 2. CSRF off    → API REST pure, pas de formulaires
 * 3. Zero Trust  → chaque requête doit porter X-User-Id + X-User-Role
 * 4. RBAC        → les droits fins sont vérifiés dans le service
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth
                // Actuator accessible sans auth (pour les probes K8s)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Tout le reste nécessite X-User-Id + X-User-Role valides
                .anyRequest().authenticated()
            )

            // Notre filtre Zero Trust s'exécute avant le filtre standard Spring
            .addFilterBefore(
                headerAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}