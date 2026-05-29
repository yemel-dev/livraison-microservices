package com.livraison.user.dto;

import com.livraison.user.model.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

/** Ce que le client envoie lors de l'inscription. */
@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;

    // Optionnel — si absent, le service attribue CLIENT par défaut
    private Role role;
}