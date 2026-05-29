package com.livraison.colis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration Swagger / OpenAPI 3 pour le colis-service.
 *
 * Interface disponible à : http://localhost:8082/swagger-ui.html
 * JSON OpenAPI à         : http://localhost:8082/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI colisServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Colis Service API")
                .description("""
                    ## Microservice de gestion des colis
                    
                    Ce service est le cœur fonctionnel de la plateforme de livraison.
                    Il gère le **cycle de vie complet des colis** :
                    création, suivi, changement de statut et suppression.
                    
                    ### Architecture Zero Trust
                    Toutes les requêtes (sauf `/actuator/health`) nécessitent les headers :
                    - `X-User-Id` : ID de l'utilisateur authentifié (injecté par l'api-gateway)
                    - `X-User-Role` : Rôle de l'utilisateur (`ROLE_CLIENT`, `ROLE_LIVREUR`, `ROLE_ADMIN`)
                    
                    ### Cycle de vie d'un colis
                    ```
                    EN_ATTENTE → ENLEVE → EN_TRANSIT → EN_LIVRAISON → LIVRE
                                                                     ↘ ECHEC_LIVRAISON
                    ```
                    
                    ### Événements Kafka publiés
                    - `colis.created` : après création réussie
                    - `colis.status_changed` : après chaque changement de statut
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Équipe Livraison Microservices")
                    .email("dev@livraison-microservices.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))

            .servers(List.of(
                new Server()
                    .url("http://localhost:8082")
                    .description("Serveur de développement local"),
                new Server()
                    .url("http://colis-service:8082")
                    .description("Serveur Docker Compose")
            ))

            // Déclaration des headers de sécurité Zero Trust
            .components(new Components()
                .addSecuritySchemes("X-User-Id",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-User-Id")
                        .description("ID de l'utilisateur authentifié (Long). Injecté par l'api-gateway."))
                .addSecuritySchemes("X-User-Role",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-User-Role")
                        .description("Rôle de l'utilisateur : ROLE_CLIENT, ROLE_LIVREUR ou ROLE_ADMIN")))

            // Appliquer les headers de sécurité à tous les endpoints
            .addSecurityItem(new SecurityRequirement()
                .addList("X-User-Id")
                .addList("X-User-Role"));
    }
}