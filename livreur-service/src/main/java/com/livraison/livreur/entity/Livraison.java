package com.livraison.livreur.entity;


import com.livraison.livreur.enums.StatutLivraison;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "livraisons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers le colis (géré par le Colis Service)
    @NotBlank(message = "Le numéro de suivi est obligatoire")
    @Column(nullable = false)
    private String numeroSuivi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livreur_id", nullable = false)
    private Livreur livreur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutLivraison statut = StatutLivraison.ASSIGNEE;

    @Column(nullable = false)
    private LocalDateTime dateAssignation;

    private LocalDateTime dateLivraison;

    private String motifEchec;

    // ─── Méthodes métier

    public void confirmerLivraison() {
        this.statut = StatutLivraison.LIVREE;
        this.dateLivraison = LocalDateTime.now();
    }

    public void enregistrerEchec(String motif) {
        this.statut = StatutLivraison.ECHEC;
        this.dateLivraison = LocalDateTime.now();
        this.motifEchec = motif;
    }

    public boolean isTermine() {
        return this.statut == StatutLivraison.LIVREE
                || this.statut == StatutLivraison.ECHEC;
    }

    @PrePersist
    public void prePersist() {
        if (this.dateAssignation == null) {
            this.dateAssignation = LocalDateTime.now();
        }
    }


}
