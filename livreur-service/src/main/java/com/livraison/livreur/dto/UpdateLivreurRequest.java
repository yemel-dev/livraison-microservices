package com.livraison.livreur.dto;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLivreurRequest {
    private String nom;
    private String prenom;

    @Pattern(regexp = "^[+0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    private String vehicule;
}
