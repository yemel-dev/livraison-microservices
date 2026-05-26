package com.livraison.colis.dto;

import com.livraison.colis.enums.OptionService;
import com.livraison.colis.enums.StatutColis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de réponse renvoyé au client après création ou consultation d'un colis.
 *
 * Contient plus d'informations que le RequestDTO :
 * - id, numeroSuivi, statut, dates (générés côté serveur)
 * - delaiLivraisonJours (calculé depuis l'enum OptionService)
 *
 * On n'expose JAMAIS l'entité Colis directement dans l'API :
 * si la BDD change, le contrat API reste stable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColisResponseDTO {

    private Long id;
    private String numeroSuivi;

    // Expéditeur
    private String expediteurNom;
    private String expediteurAdresse;
    private String expediteurEmail;

    // Destinataire
    private String destinataireNom;
    private String destinataireAdresse;
    private String destinataireEmail;

    // Caractéristiques
    private Double poids;
    private String description;
    private OptionService optionService;
    private int delaiLivraisonJours;    // Calculé depuis optionService.getDelaiJours()

    // Statut et audit
    private StatutColis statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateMiseAJour;
    private Long createdByUserId;
}