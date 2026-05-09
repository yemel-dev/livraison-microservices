package com.livraison.livreur.dto;
// ce que l'Api renvoir pour un livreur


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//ce que l'API renvoie pour une livraison

public class LivreurResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String telephone;
    private String vehicule;
    private boolean actif;

}
