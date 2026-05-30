package com.livraison.livreur.kafka;

import com.livraison.livreur.enums.StatutColis;
import lombok.*;
import java.time.LocalDateTime;

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
    private Long livreurId;
    private String livreurNom;
}