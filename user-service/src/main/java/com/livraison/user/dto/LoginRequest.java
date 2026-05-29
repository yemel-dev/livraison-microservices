package com.livraison.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Ce que le client envoie lors de la connexion. */
@Data
public class LoginRequest {

    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;
}