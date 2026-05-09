package com.livraison.livreur.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//  pour enregistrer un échec (motifEchec)

public class EchecRequest {
    @NotBlank(message = "Le motif d'échec est obligatoire")
    private String motifEchec;
}
