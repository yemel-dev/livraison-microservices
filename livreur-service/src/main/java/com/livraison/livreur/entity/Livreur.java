package com.livraison.livreur.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "livreurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * userId = l'ID de l'utilisateur dans le user-service (table users).
     * Permet de faire le lien entre le compte utilisateur et le profil livreur.
     * Injecté par le Gateway via le header X-User-Id.
     */
    @Column(nullable = true)
    private Long userId;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Column(nullable = false)
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[+0-9]{8,15}$", message = "Numéro de téléphone invalide")
    @Column(nullable = false, unique = true)
    private String telephone;

    @NotBlank(message = "Le véhicule est obligatoire")
    @Column(nullable = false)
    private String vehicule;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @OneToMany(mappedBy = "livreur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Livraison> livraisons;

    public Long getId() { return id; }
    public boolean isActif() { return actif; }
}