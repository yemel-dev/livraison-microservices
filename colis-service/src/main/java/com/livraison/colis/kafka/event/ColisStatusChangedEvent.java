package com.livraison.colis.kafka.event;

import com.livraison.colis.enums.StatutColis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement publié sur le topic Kafka "colis.status_changed"
 * à chaque changement de statut d'un colis.
 *
 * Consommateurs attendus :
 * - notification-service → notifie le destinataire de l'avancement de sa livraison
 * - livreur-service      → peut réagir aux changements de statut si nécessaire
 *
 * Contient l'ancien ET le nouveau statut pour que le consommateur
 * puisse construire un message contextualisé (ex: "Votre colis est passé
 * de EN_TRANSIT à EN_LIVRAISON").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColisStatusChangedEvent {

    /** ID technique du colis */
    private Long colisId;

    /** Numéro de suivi lisible */
    private String numeroSuivi;

    /** Transition de statut */
    private StatutColis ancienStatut;
    private StatutColis nouveauStatut;

    /** Informations destinataire pour la notification */
    private String destinataireEmail;
    private String destinataireNom;

    /** Date de la mise à jour */
    private LocalDateTime dateMiseAJour;

    /** ID de l'utilisateur qui a effectué le changement */
    private Long modifiePar;
}