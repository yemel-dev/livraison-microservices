package com.livraison.colis.controller;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.dto.UpdateStatutDTO;
import com.livraison.colis.service.ColisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colis")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Colis",
    description = """
        Gestion complète du cycle de vie des colis.
        Création, consultation, mise à jour, changement de statut et suppression.
        
        **Headers obligatoires sur chaque requête :**
        - `X-User-Id` : ID de l'utilisateur (Long)
        - `X-User-Role` : ROLE_CLIENT | ROLE_LIVREUR | ROLE_ADMIN
        """
)
public class ColisController {

    private final ColisService colisService;

    // ─── POST /api/colis ─────────────────────────────────────────────────────

    @Operation(
        summary = "Créer un nouveau colis",
        description = """
            Crée un nouveau colis avec statut initial **EN_ATTENTE**.
            
            - Le numéro de suivi est **généré automatiquement** (format : COL-YYYYMMDD-XXXXX)
            - Le statut est toujours initialisé à **EN_ATTENTE**
            - Un événement **colis.created** est publié sur Kafka après la création
            
            **Accès :** ROLE_CLIENT, ROLE_ADMIN
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Colis créé avec succès",
            content = @Content(schema = @Schema(implementation = ColisResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "numeroSuivi": "COL-20260529-A3F7K",
                      "expediteurNom": "Alice Dupont",
                      "expediteurAdresse": "12 rue de Paris, 75001 Paris",
                      "expediteurEmail": "alice@email.com",
                      "destinataireNom": "Bob Martin",
                      "destinataireAdresse": "5 avenue de Lyon, 69001 Lyon",
                      "destinataireEmail": "bob@email.com",
                      "poids": 2.5,
                      "optionService": "EXPRESS",
                      "delaiLivraisonJours": 2,
                      "statut": "EN_ATTENTE",
                      "dateCreation": "2026-05-29T10:30:00",
                      "createdByUserId": 1
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Données invalides (validation échouée)",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 400,
                  "error": "Données invalides",
                  "erreurs": {
                    "poids": "Le poids est obligatoire",
                    "expediteurEmail": "L'email de l'expéditeur n'est pas valide"
                  }
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé")
    })
    @PostMapping
    public ResponseEntity<ColisResponseDTO> creerColis(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Données du colis à créer",
                content = @Content(examples = @ExampleObject(value = """
                    {
                      "expediteurNom": "Alice Dupont",
                      "expediteurAdresse": "12 rue de Paris, 75001 Paris",
                      "expediteurEmail": "alice@email.com",
                      "destinataireNom": "Bob Martin",
                      "destinataireAdresse": "5 avenue de Lyon, 69001 Lyon",
                      "destinataireEmail": "bob@email.com",
                      "poids": 2.5,
                      "description": "Colis fragile",
                      "optionService": "EXPRESS"
                    }
                    """)))
            ColisRequestDTO dto,
            @Parameter(description = "ID de l'utilisateur authentifié", required = true, example = "1")
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Rôle de l'utilisateur", required = true, example = "ROLE_CLIENT")
            @RequestHeader("X-User-Role") String role) {

        log.info("POST /api/colis - userId={}, role={}", userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(colisService.creerColis(dto, userId));
    }

    // ─── GET /api/colis ──────────────────────────────────────────────────────

    @Operation(
        summary = "Lister les colis",
        description = """
            Retourne la liste des colis selon le rôle :
            - **ROLE_CLIENT** → uniquement ses propres colis (filtrés par X-User-Id)
            - **ROLE_ADMIN** → tous les colis de tous les utilisateurs
            
            **Accès :** ROLE_CLIENT, ROLE_ADMIN
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des colis retournée avec succès"),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant")
    })
    @GetMapping
    public ResponseEntity<List<ColisResponseDTO>> getAllColis(
            @Parameter(description = "ID de l'utilisateur", required = true, example = "1")
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Rôle de l'utilisateur", required = true, example = "ROLE_CLIENT")
            @RequestHeader("X-User-Role") String role) {

        log.info("GET /api/colis - userId={}, role={}", userId, role);
        return ResponseEntity.ok(colisService.getAllColis(userId, role));
    }

    // ─── GET /api/colis/{id} ─────────────────────────────────────────────────

    @Operation(
        summary = "Récupérer un colis par ID",
        description = """
            Retourne les détails d'un colis par son identifiant technique.
            
            - **ROLE_CLIENT** : accès uniquement à ses propres colis
            - **ROLE_ADMIN** : accès à tous les colis
            
            **Accès :** ROLE_CLIENT (owner), ROLE_ADMIN
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Colis trouvé"),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — colis appartient à un autre utilisateur",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 403,
                  "error": "Accès non autorisé",
                  "message": "Vous n'avez pas accès à ce colis."
                }
                """))),
        @ApiResponse(responseCode = "404", description = "Colis introuvable",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 404,
                  "error": "Colis introuvable",
                  "message": "Colis introuvable avec l'id : 9999",
                  "id": 9999
                }
                """)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ColisResponseDTO> getColisById(
            @Parameter(description = "ID technique du colis", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID de l'utilisateur", required = true, example = "1")
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Rôle de l'utilisateur", required = true, example = "ROLE_CLIENT")
            @RequestHeader("X-User-Role") String role) {

        log.info("GET /api/colis/{} - userId={}", id, userId);
        return ResponseEntity.ok(colisService.getColisById(id, userId, role));
    }

    // ─── GET /api/colis/suivi/{numeroSuivi} ──────────────────────────────────

    @Operation(
        summary = "Rechercher un colis par numéro de suivi",
        description = """
            Recherche un colis via son numéro de suivi lisible.
            
            Format du numéro de suivi : **COL-YYYYMMDD-XXXXX**
            Exemple : `COL-20260529-A3F7K`
            
            **Accès :** Tout utilisateur authentifié
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Colis trouvé"),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant"),
        @ApiResponse(responseCode = "404", description = "Numéro de suivi introuvable")
    })
    @GetMapping("/suivi/{numeroSuivi}")
    public ResponseEntity<ColisResponseDTO> getColisByNumeroSuivi(
            @Parameter(description = "Numéro de suivi du colis", required = true, example = "COL-20260529-A3F7K")
            @PathVariable String numeroSuivi,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {

        log.info("GET /api/colis/suivi/{}", numeroSuivi);
        return ResponseEntity.ok(colisService.getColisByNumeroSuivi(numeroSuivi));
    }

    // ─── PUT /api/colis/{id} ─────────────────────────────────────────────────

    @Operation(
        summary = "Mettre à jour un colis",
        description = """
            Met à jour les informations d'un colis.
            
            **Contraintes :**
            - Le colis doit être en statut **EN_ATTENTE** (non encore pris en charge)
            - Seul le propriétaire ou un ADMIN peut modifier
            - Le numéro de suivi et l'ID ne sont jamais modifiés
            
            **Accès :** ROLE_CLIENT (owner), ROLE_ADMIN
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Colis mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Modification impossible — statut non EN_ATTENTE ou données invalides",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 400,
                  "error": "Opération non autorisée",
                  "message": "Modification impossible : statut actuel = EN_TRANSIT."
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Colis introuvable")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ColisResponseDTO> updateColis(
            @Parameter(description = "ID du colis à modifier", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ColisRequestDTO dto,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {

        log.info("PUT /api/colis/{} - userId={}", id, userId);
        return ResponseEntity.ok(colisService.updateColis(id, dto, userId, role));
    }

    // ─── PATCH /api/colis/{id}/statut ────────────────────────────────────────

    @Operation(
        summary = "Changer le statut d'un colis",
        description = """
            Applique une transition de statut selon le cycle de vie strict :
            
            ```
            EN_ATTENTE → ENLEVE → EN_TRANSIT → EN_LIVRAISON → LIVRE
                                                             ↘ ECHEC_LIVRAISON
            ```
            
            Toute transition non conforme lève une erreur 400.
            Un événement **colis.status_changed** est publié sur Kafka.
            
            **Accès :** ROLE_LIVREUR, ROLE_ADMIN uniquement
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Transition de statut invalide",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 400,
                  "error": "Transition de statut invalide",
                  "message": "Transition invalide : EN_ATTENTE → LIVRE",
                  "statutActuel": "EN_ATTENTE",
                  "statutCible": "LIVRE"
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant — LIVREUR ou ADMIN requis"),
        @ApiResponse(responseCode = "404", description = "Colis introuvable")
    })
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ColisResponseDTO> updateStatut(
            @Parameter(description = "ID du colis", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Nouveau statut souhaité",
                content = @Content(examples = @ExampleObject(value = """
                    { "nouveauStatut": "ENLEVE" }
                    """)))
            UpdateStatutDTO updateStatutDTO,
            @Parameter(description = "ID du livreur/admin", required = true, example = "10")
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Rôle requis : ROLE_LIVREUR ou ROLE_ADMIN", required = true, example = "ROLE_LIVREUR")
            @RequestHeader("X-User-Role") String role) {

        log.info("PATCH /api/colis/{}/statut → {}", id, updateStatutDTO.getNouveauStatut());
        return ResponseEntity.ok(colisService.updateStatut(id, updateStatutDTO.getNouveauStatut(), userId, role));
    }

    // ─── DELETE /api/colis/{id} ──────────────────────────────────────────────

    @Operation(
        summary = "Supprimer un colis",
        description = """
            Supprime définitivement un colis.
            
            **Contraintes :**
            - Le colis doit être en statut **EN_ATTENTE** uniquement
            - Réservé aux **ADMIN** exclusivement
            
            **Accès :** ROLE_ADMIN uniquement
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Colis supprimé avec succès (corps vide)"),
        @ApiResponse(responseCode = "400", description = "Suppression impossible — colis non EN_ATTENTE",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 400,
                  "error": "Opération non autorisée",
                  "message": "Suppression impossible : statut actuel = LIVRE."
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Header X-User-Id manquant"),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant — ADMIN requis"),
        @ApiResponse(responseCode = "404", description = "Colis introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColis(
            @Parameter(description = "ID du colis à supprimer", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID de l'admin", required = true, example = "99")
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Rôle requis : ROLE_ADMIN", required = true, example = "ROLE_ADMIN")
            @RequestHeader("X-User-Role") String role) {

        log.info("DELETE /api/colis/{} - userId={}", id, userId);
        colisService.deleteColis(id, userId, role);
        return ResponseEntity.noContent().build();
    }
}