package com.livraison.livreur.dto;

import com.livraison.livreur.enums.StatutLivraison;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivraisonResponse {
    private Long id;
    private String numeroSuivi;
    private Long livreurId;
    private String livreurNom;
    private StatutLivraison statut;
    private LocalDateTime dateAssignation;
    private LocalDateTime dateLivraison;
    private String motifEchec;

}
