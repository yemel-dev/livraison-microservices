package com.livraison.colis.dto;
import com.livraison.colis.enums.OptionService;
import com.livraison.colis.enums.StatutColis;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Réponse complète d'un colis")
public class ColisResponseDTO {
    @Schema(description = "Identifiant technique du colis", example = "1")
    private Long id;
    @Schema(description = "Numéro de suivi unique généré par le serveur", example = "COL-20260529-A3F7K")
    private String numeroSuivi;
    @Schema(description = "Nom de l'expéditeur", example = "Alice Dupont")
    private String expediteurNom;
    @Schema(description = "Adresse de l'expéditeur", example = "12 rue de Paris, 75001 Paris")
    private String expediteurAdresse;
    @Schema(description = "Email de l'expéditeur", example = "alice@email.com")
    private String expediteurEmail;
    @Schema(description = "Nom du destinataire", example = "Bob Martin")
    private String destinataireNom;
    @Schema(description = "Adresse du destinataire", example = "5 avenue de Lyon, 69001 Lyon")
    private String destinataireAdresse;
    @Schema(description = "Email du destinataire", example = "bob@email.com")
    private String destinataireEmail;
    @Schema(description = "Poids en kilogrammes", example = "2.5")
    private Double poids;
    @Schema(description = "Description du contenu", example = "Colis fragile")
    private String description;
    @Schema(description = "Option de service choisie", example = "EXPRESS")
    private OptionService optionService;
    @Schema(description = "Délai de livraison estimé en jours selon l'option choisie", example = "2")
    private int delaiLivraisonJours;
    @Schema(description = "Statut actuel du colis dans son cycle de vie", example = "EN_ATTENTE")
    private StatutColis statut;
    @Schema(description = "Date et heure de création du colis", example = "2026-05-29T10:30:00")
    private LocalDateTime dateCreation;
    @Schema(description = "Date et heure de la dernière mise à jour", example = "2026-05-29T14:00:00")
    private LocalDateTime dateMiseAJour;
    @Schema(description = "ID de l'utilisateur créateur du colis", example = "1")
    private Long createdByUserId;
    @Schema(description = "ID du livreur assigné", example = "1")
    private Long livreurId;
    @Schema(description = "Nom complet du livreur assigné", example = "tawat amine")
    private String livreurNom;
}