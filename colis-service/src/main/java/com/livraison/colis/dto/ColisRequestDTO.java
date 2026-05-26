package com.livraison.colis.dto;

import com.livraison.colis.enums.OptionService;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de création/mise à jour d'un colis.
 * Contient les données fournies par le client via POST /api/colis.
 *
 * Règles :
 * - Le numeroSuivi n'est PAS ici : il est généré côté serveur.
 * - Le statut n'est PAS ici : il démarre toujours à EN_ATTENTE.
 * - Le createdByUserId n'est PAS ici : il vient du header X-User-Id.
 * - Toutes les annotations @Valid sont validées automatiquement par Spring
 *   quand le controller utilise @Valid sur le paramètre @RequestBody.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColisRequestDTO {

    // ─── Expéditeur ──────────────────────────────────────────────────────────

    @NotBlank(message = "Le nom de l'expéditeur est obligatoire")
    @Size(max = 100, message = "Le nom de l'expéditeur ne peut pas dépasser 100 caractères")
    private String expediteurNom;

    @NotBlank(message = "L'adresse de l'expéditeur est obligatoire")
    private String expediteurAdresse;

    @NotBlank(message = "L'email de l'expéditeur est obligatoire")
    @Email(message = "L'email de l'expéditeur n'est pas valide")
    private String expediteurEmail;

    // ─── Destinataire ────────────────────────────────────────────────────────

    @NotBlank(message = "Le nom du destinataire est obligatoire")
    @Size(max = 100, message = "Le nom du destinataire ne peut pas dépasser 100 caractères")
    private String destinataireNom;

    @NotBlank(message = "L'adresse du destinataire est obligatoire")
    private String destinataireAdresse;

    @Email(message = "L'email du destinataire n'est pas valide")
    private String destinataireEmail;   // Optionnel

    // ─── Caractéristiques ────────────────────────────────────────────────────

    @NotNull(message = "Le poids est obligatoire")
    @Positive(message = "Le poids doit être un nombre positif")
    private Double poids;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;         // Optionnel

    @NotNull(message = "L'option de service est obligatoire")
    private OptionService optionService;
}