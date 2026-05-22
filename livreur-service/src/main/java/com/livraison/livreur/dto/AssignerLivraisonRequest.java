package com.livraison.livreur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignerLivraisonRequest {

    @NotBlank(message = "Le numéro de suivi est obligatoire")
    private String numeroSuivi;

    @NotNull(message = "L'ID du livreur est obligatoire")
    private Long livreurId;
}
