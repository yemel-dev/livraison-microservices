package com.livraison.livreur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLivreurRequest {

    /**
     * userId = l'ID de l'utilisateur dans le user-service.
     * Permet de lier le profil livreur au compte utilisateur.
     * L'ADMIN le récupère depuis le token ou le fournit manuellement.
     */
    private Long userId;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[+0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    @NotBlank(message = "Le véhicule est obligatoire")
    private String vehicule;

}