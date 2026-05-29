package com.livraison.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger UI.
 * Ajoute le bouton "Authorize" pour tester les endpoints avec un Bearer Token.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User Service API — Plateforme Livraisons")
                .description("Authentification JWT — inscription, login, gestion des utilisateurs")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Équipe Livraison M1")
                ))
            // Déclare le schéma Bearer Token
            .components(new Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Colle ton token JWT ici sans le préfixe 'Bearer'")
                ))
            // Active le bouton Authorize sur tous les endpoints
            .addSecurityItem(new SecurityRequirement().addList("Bearer"));
    }
}