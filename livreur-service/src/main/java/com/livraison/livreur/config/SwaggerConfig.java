package com.livraison.livreur.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger/OpenAPI pour le Livreur Service.
 * Accessible sur : http://localhost:8083/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI livreurServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Livreur Service API")
                        .description("""
                                Microservice de gestion des livreurs et des tournées de livraison.
                                
                                **Rôles :**
                                - `ADMIN` — gestion complète des livreurs et assignation des colis
                                - `LIVREUR` — consultation et mise à jour de ses propres livraisons
                                
                                **Authentification :** JWT injecté par le Gateway via header `X-User-Id` et `X-User-Role`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe Livraison")
                                .email("dev@livraison.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT fourni par le Gateway via X-User-Id")));
    }
}