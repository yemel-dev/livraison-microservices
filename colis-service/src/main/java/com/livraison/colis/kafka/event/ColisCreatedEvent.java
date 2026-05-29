package com.livraison.colis.kafka.event;

import com.livraison.colis.enums.OptionService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement publié sur le topic Kafka "colis.created"
 * immédiatement après la création réussie d'un colis en BDD.
 *
 * Consommateurs attendus :
 * - notification-service → envoie un email/SMS de confirmation à l'expéditeur
 *
 * Règle : cet objet est IMMUABLE en pratique (on ne le modifie pas après création).
 * Il contient tous les champs dont le consommateur a besoin,
 * sans qu'il ait à rappeler le colis-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColisCreatedEvent {

    /** ID technique du colis en BDD */
    private Long colisId;

    /** Numéro de suivi lisible (ex: COL-20240315-A3F7K) */
    private String numeroSuivi;

    /** Informations destinataire pour l'envoi de notification */
    private String destinataireEmail;
    private String destinataireNom;

    /** Informations expéditeur */
    private String expediteurNom;
    private String expediteurEmail;

    /** Option choisie — permet au consommateur d'afficher le délai estimé */
    private OptionService optionService;
    private int delaiLivraisonJours;

    /** Date de création pour l'affichage dans la notification */
    private LocalDateTime dateCreation;

    /** ID de l'utilisateur créateur */
    private Long createdByUserId;
}