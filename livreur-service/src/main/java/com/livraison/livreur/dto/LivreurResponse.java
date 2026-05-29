package com.livraison.livreur.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivreurResponse {

    private Long id;
    private Long userId;   // ← lien avec le user-service
    private String nom;
    private String prenom;
    private String telephone;
    private String vehicule;
    private boolean actif;

}