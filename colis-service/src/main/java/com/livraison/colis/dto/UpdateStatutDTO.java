package com.livraison.colis.dto;

import com.livraison.colis.enums.StatutColis;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload pour changer le statut d'un colis")
public class UpdateStatutDTO {

    @Schema(
        description = """
            Nouveau statut souhaité. Doit respecter le cycle de vie :
            EN_ATTENTE → ENLEVE → EN_TRANSIT → EN_LIVRAISON → LIVRE / ECHEC_LIVRAISON
            """,
        example = "ENLEVE",
        allowableValues = {"EN_ATTENTE", "ENLEVE", "EN_TRANSIT", "EN_LIVRAISON", "LIVRE", "ECHEC_LIVRAISON"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Le nouveau statut est obligatoire")
    private StatutColis nouveauStatut;
}