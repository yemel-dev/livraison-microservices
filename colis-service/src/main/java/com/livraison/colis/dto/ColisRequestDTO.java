package com.livraison.colis.dto;

import com.livraison.colis.enums.OptionService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Données de création ou mise à jour d'un colis")
public class ColisRequestDTO {

    @Schema(description = "Nom complet de l'expéditeur", example = "Alice Dupont", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Le nom de l'expéditeur est obligatoire")
    @Size(max = 100, message = "Le nom de l'expéditeur ne peut pas dépasser 100 caractères")
    private String expediteurNom;

    @Schema(description = "Adresse complète de l'expéditeur", example = "12 rue de Paris, 75001 Paris", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "L'adresse de l'expéditeur est obligatoire")
    private String expediteurAdresse;

    @Schema(description = "Email de l'expéditeur", example = "alice@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "L'email de l'expéditeur est obligatoire")
    @Email(message = "L'email de l'expéditeur n'est pas valide")
    private String expediteurEmail;

    @Schema(description = "Nom complet du destinataire", example = "Bob Martin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Le nom du destinataire est obligatoire")
    @Size(max = 100, message = "Le nom du destinataire ne peut pas dépasser 100 caractères")
    private String destinataireNom;

    @Schema(description = "Adresse complète du destinataire", example = "5 avenue de Lyon, 69001 Lyon", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "L'adresse du destinataire est obligatoire")
    private String destinataireAdresse;

    @Schema(description = "Email du destinataire (optionnel)", example = "bob@email.com")
    @Email(message = "L'email du destinataire n'est pas valide")
    private String destinataireEmail;

    @Schema(description = "Poids du colis en kilogrammes", example = "2.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Le poids est obligatoire")
    @Positive(message = "Le poids doit être un nombre positif")
    private Double poids;

    @Schema(description = "Description libre du contenu (optionnel)", example = "Colis fragile - matériel électronique")
    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    @Schema(description = "Option de livraison choisie", example = "EXPRESS",
        allowableValues = {"STANDARD", "EXPRESS", "ECONOMIQUE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "L'option de service est obligatoire")
    private OptionService optionService;
}