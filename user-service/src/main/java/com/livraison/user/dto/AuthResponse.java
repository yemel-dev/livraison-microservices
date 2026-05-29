package com.livraison.user.dto;

import lombok.*;

/** Ce que le serveur retourne après login ou register — contient le token JWT. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";   // Préfixe standard JWT

    private Long   userId;
    private String nom;
    private String email;
    private String role;
}