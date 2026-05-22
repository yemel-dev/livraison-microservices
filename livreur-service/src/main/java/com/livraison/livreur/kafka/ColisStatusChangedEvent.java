package com.livraison.livreur.kafka;

import com.livraison.livreur.enums.StatutColis;
import lombok.*;

import java.time.LocalDateTime;

// ── Événement publié à chaque changement de statut d'un colis

/** en gros:
 * l'objet publié sur colis.status_changed à chaque changement de
 * statut. Contient : numeroSuivi, ancienStatut, nouveauStatut, dateChangement.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColisStatusChangedEvent {
    private String numeroSuivi;
    private StatutColis ancienStatut;
    private StatutColis nouveauStatut;
    private LocalDateTime dateChangement;
    private String eventType = "CHANGEMENT_STATUT";
}
