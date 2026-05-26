package com.livraison.colis.entity;

import com.livraison.colis.enums.OptionService;
import com.livraison.colis.enums.StatutColis;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant la table `colis` en base de données.
 *
 * Points clés :
 * - @Enumerated(EnumType.STRING) : les enums sont stockés en clair ("EN_ATTENTE", "EXPRESS", etc.)
 *   et non comme entiers — plus lisible et robuste si l'ordre de l'enum change.
 * - numeroSuivi a une contrainte UNIQUE en BDD (unique = true sur @Column).
 * - Les timestamps sont gérés automatiquement par Hibernate.
 * - createdByUserId est injecté depuis le header X-User-Id (Zero Trust).
 *   Il n'est PAS une Foreign Key vers user-service (pas de couplage entre microservices en BDD).
 */
@Entity
@Table(
    name = "colis",
    indexes = {
        @Index(name = "idx_numero_suivi",    columnList = "numero_suivi"),
        @Index(name = "idx_statut",          columnList = "statut"),
        @Index(name = "idx_created_by_user", columnList = "created_by_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Colis {

    // ─── Clé primaire ────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Numéro de suivi ─────────────────────────────────────────────────────
    /**
     * Généré côté serveur (dans le service), jamais fourni par le client.
     * Format : COL-YYYYMMDD-XXXXX (ex : COL-20240315-A3F7K)
     */
    @Column(name = "numero_suivi", nullable = false, unique = true, length = 50)
    private String numeroSuivi;

    // ─── Expéditeur ──────────────────────────────────────────────────────────
    @Column(name = "expediteur_nom", nullable = false, length = 100)
    private String expediteurNom;

    @Column(name = "expediteur_adresse", nullable = false, columnDefinition = "TEXT")
    private String expediteurAdresse;

    @Column(name = "expediteur_email", nullable = false, length = 150)
    private String expediteurEmail;

    // ─── Destinataire ────────────────────────────────────────────────────────
    @Column(name = "destinataire_nom", nullable = false, length = 100)
    private String destinataireNom;

    @Column(name = "destinataire_adresse", nullable = false, columnDefinition = "TEXT")
    private String destinataireAdresse;

    @Column(name = "destinataire_email", length = 150)
    private String destinataireEmail;   // Optionnel selon le diagramme UML

    // ─── Caractéristiques du colis ───────────────────────────────────────────
    @Column(name = "poids", nullable = false)
    private Double poids;               // En kilogrammes

    @Column(name = "description", length = 500)
    private String description;         // Optionnel

    @Enumerated(EnumType.STRING)
    @Column(name = "option_service", nullable = false, length = 20)
    private OptionService optionService;

    // ─── Statut ──────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutColis statut = StatutColis.EN_ATTENTE;  // Valeur par défaut à la création

    // ─── Audit ───────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;

    /**
     * ID de l'utilisateur qui a créé ce colis.
     * Extrait du header HTTP X-User-Id injecté par l'api-gateway (Zero Trust).
     * PAS de Foreign Key : les microservices ne partagent pas leur BDD.
     */
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    // ─── Méthodes métier ─────────────────────────────────────────────────────

    /**
     * Change le statut du colis.
     * La validation de la transition doit être faite AVANT d'appeler cette méthode
     * (dans ColisServiceImpl), en utilisant StatutColis.peutTransitionnerVers().
     *
     * @param nouveauStatut le nouveau statut à appliquer
     */
    public void changerStatut(StatutColis nouveauStatut) {
        this.statut = nouveauStatut;
    }

    /**
     * Indique si le colis a atteint un statut terminal
     * (LIVRE ou ECHEC_LIVRAISON — plus aucune modification possible).
     *
     * @return true si le colis est dans un état final
     */
    public boolean isTermine() {
        return this.statut != null && this.statut.estTerminal();
    }

    /**
     * Retourne le numéro de suivi (méthode explicite conforme au diagramme UML).
     */
    public String getNumeroSuivi() {
        return this.numeroSuivi;
    }

    /**
     * Retourne le statut courant (méthode explicite conforme au diagramme UML).
     */
    public StatutColis getStatut() {
        return this.statut;
    }
}