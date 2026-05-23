package com.livraison.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayConfig {

    // -------------------------------------------------------------------------
    // BEAN CORS — autorise les appels cross-origin depuis le navigateur
    //
    // ATTENTION : on utilise CorsWebFilter du package .reactive
    // PAS org.springframework.web.filter.CorsFilter (qui est Spring MVC)
    // Spring Cloud Gateway tourne sur WebFlux — les deux sont incompatibles
    // -------------------------------------------------------------------------
    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées
        // En développement : tout autoriser
        // En production : remplacer par l'URL exacte du frontend
        //   ex : config.addAllowedOrigin("https://livraison.monapp.com");
        config.addAllowedOrigin("*");

        // Méthodes HTTP autorisées
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS"); // preflight CORS obligatoire

        // Headers autorisés dans les requêtes entrantes
        config.addAllowedHeader("Authorization");   // token JWT
        config.addAllowedHeader("Content-Type");    // JSON body
        config.addAllowedHeader("X-User-Id");       // injecté par le Gateway
        config.addAllowedHeader("X-User-Role");     // injecté par le Gateway
        config.addAllowedHeader("*");               // tout autre header

        // Headers exposés dans les réponses
        // Le navigateur peut les lire depuis le code JavaScript côté client
        config.addExposedHeader("X-User-Id");
        config.addExposedHeader("X-User-Role");

        // allowCredentials DOIT être false quand allowedOrigin = "*"
        // Les deux sont incompatibles — le navigateur refuse la combinaison
        config.setAllowCredentials(false);

        // Le navigateur met en cache le résultat du preflight OPTIONS pendant 1h
        // Évite d'envoyer une requête OPTIONS avant chaque appel API
        config.setMaxAge(3600L);

        // Appliquer cette configuration CORS sur tous les chemins du Gateway
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}