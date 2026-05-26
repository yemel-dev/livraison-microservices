package com.livraison.colis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration Spring Security TEMPORAIRE.
 *
 * ⚠️  Cette configuration autorise toutes les requêtes sans authentification.
 *     Elle sera REMPLACÉE à l'Étape 14 par la vraie config Zero Trust
 *     avec HeaderAuthenticationFilter.
 *
 * Pourquoi elle est nécessaire maintenant :
 * - spring-boot-starter-security bloque tout par défaut (401 sur chaque requête)
 * - On veut tester le CRUD avant d'implémenter la sécurité
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF : API REST stateless, pas de formulaires HTML
            .csrf(AbstractHttpConfigurer::disable)

            // Pas de session HTTP côté serveur (stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // TODO Étape 14 — Remplacer par les vraies règles RBAC :
            // .authorizeHttpRequests(auth -> auth
            //     .requestMatchers("/api/colis/suivi/**").permitAll()
            //     .anyRequest().authenticated()
            // )
            // + addFilterBefore(headerAuthFilter, ...)

            // Pour l'instant : tout autoriser sans authentification
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}