package com.livraison.colis.dto;

import com.livraison.colis.enums.StatutColis;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilisé pour PATCH /api/colis/{id}/statut.
 * Contient uniquement le nouveau statut souhaité.
 *
 * Exemple de body JSON :
 * {
 *   "nouveauStatut": "ENLEVE"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatutDTO {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private StatutColis nouveauStatut;
}