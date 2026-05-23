package com.livraison.gateway.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;

    private String message;

    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Méthode factory statique.
     * Crée un ErrorResponse avec le timestamp automatiquement rempli à maintenant.
     * Utilisée par tous les filtres : JwtAuthenticationFilter et RateLimitingFilter.
     *
     * Exemple d'usage :
     *   ErrorResponse.of(401, "Token JWT manquant", "/api/colis")
     */
    public static ErrorResponse of(int status, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}