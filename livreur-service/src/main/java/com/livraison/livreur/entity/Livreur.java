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

    // Un livreur a plusieurs livraisons
    @OneToMany(mappedBy = "livreur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Livraison> livraisons;

    public Long getId() { return id; }
    public boolean isActif() { return actif; }

}
