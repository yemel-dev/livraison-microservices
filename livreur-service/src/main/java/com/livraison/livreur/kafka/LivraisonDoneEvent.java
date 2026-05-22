package com.livraison.livreur.kafka;

import lombok.*;

import java.time.LocalDateTime;

//Événement publié quand une livraison est confirmée

/** en gros :  l'objet publié sur le topic livraison.done quand une livraison
 *  est confirmée. Contient : numeroSuivi, livreurId, livreurNom, dateLivraison.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivraisonDoneEvent {

    private String numeroSuivi;
    private Long livreurId;
    private String livreurNom;
    private LocalDateTime dateLivraison;
    private String eventType = "LIVRAISON_CONFIRMEE";
}


