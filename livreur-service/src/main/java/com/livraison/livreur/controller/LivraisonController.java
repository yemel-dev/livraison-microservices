package com.livraison.livreur.controller;

import com.livraison.livreur.dto.AssignerLivraisonRequest;
import com.livraison.livreur.dto.EchecRequest;
import com.livraison.livreur.dto.LivraisonResponse;
import com.livraison.livreur.security.SecurityContext;
import com.livraison.livreur.service.LivraisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Livraisons", description = "Gestion des livraisons et tournées — ADMIN pour assignation, LIVREUR pour suivi")
public class LivraisonController {

    private final LivraisonService livraisonService;

    // -------------------------------------------------------------------------
    // POST /api/livraisons
    // -------------------------------------------------------------------------
    @PostMapping("/api/livraisons")
    @Operation(
        summary = "Assigner un colis à un livreur",
        description = "Crée une livraison en associant un colis (par numéro de suivi) à un livreur. Réservé aux ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Livraison créée et colis assigné"),
        @ApiResponse(responseCode = "400", description = "Numéro de suivi ou livreurId manquant"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    public ResponseEntity<LivraisonResponse> assignerColis(
            @Valid @RequestBody AssignerLivraisonRequest request) {
        verifierAdmin();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(livraisonService.assignerColis(request));
    }

    // -------------------------------------------------------------------------
    // GET /api/livraisons/{id}
    // -------------------------------------------------------------------------
    @GetMapping("/api/livraisons/{id}")
    @Operation(summary = "Détail d'une livraison", description = "Retourne les informations complètes d'une livraison par son ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Livraison trouvée"),
        @ApiResponse(responseCode = "404", description = "Livraison introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<LivraisonResponse> getLivraison(
            @Parameter(description = "ID de la livraison", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraison(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/livreurs/{livreurId}/tournee
    // -------------------------------------------------------------------------
    @GetMapping("/api/livreurs/{livreurId}/tournee")
    @Operation(
        summary = "Tournée du jour d'un livreur",
        description = "Retourne les livraisons assignées aujourd'hui au livreur. Un livreur ne peut voir que sa propre tournée."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tournée retournée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — un livreur ne peut voir que sa tournée"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable")
    })
    public ResponseEntity<List<LivraisonResponse>> getTourneeJour(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long livreurId) {
        return ResponseEntity.ok(livraisonService.getTourneeJour(livreurId));
    }

    // -------------------------------------------------------------------------
    // GET /api/livreurs/{livreurId}/livraisons
    // -------------------------------------------------------------------------
    @GetMapping("/api/livreurs/{livreurId}/livraisons")
    @Operation(
        summary = "Historique des livraisons d'un livreur",
        description = "Retourne toutes les livraisons (passées et en cours) assignées à ce livreur."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique retourné"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable")
    })
    public ResponseEntity<List<LivraisonResponse>> getLivraisonsLivreur(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long livreurId) {
        return ResponseEntity.ok(livraisonService.getLivraisonsLivreur(livreurId));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/livraisons/{id}/scanner/{livreurId}
    // -------------------------------------------------------------------------
    @PatchMapping("/api/livraisons/{id}/scanner/{livreurId}")
    @Operation(
        summary = "Scanner — prise en charge du colis",
        description = "Le livreur scanne le colis pour confirmer qu'il l'a pris en charge. Passe le statut à EN_COURS."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Colis pris en charge"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — vous ne pouvez agir que sur vos propres livraisons"),
        @ApiResponse(responseCode = "404", description = "Livraison introuvable")
    })
    public ResponseEntity<LivraisonResponse> scanner(
            @Parameter(description = "ID de la livraison", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID du livreur", required = true, example = "1") @PathVariable Long livreurId) {
        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.scannerPriseEnCharge(id, livreurId));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/livraisons/{id}/transit/{livreurId}
    // -------------------------------------------------------------------------
    @PatchMapping("/api/livraisons/{id}/transit/{livreurId}")
    @Operation(
        summary = "Mettre en transit",
        description = "Le livreur indique que le colis est en route vers le destinataire. Passe le statut à EN_TRANSIT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut mis à EN_TRANSIT"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Livraison introuvable")
    })
    public ResponseEntity<LivraisonResponse> mettreEnTransit(
            @Parameter(description = "ID de la livraison", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID du livreur", required = true, example = "1") @PathVariable Long livreurId) {
        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.mettreEnTransit(id, livreurId));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/livraisons/{id}/confirmer/{livreurId}
    // -------------------------------------------------------------------------
    @PatchMapping("/api/livraisons/{id}/confirmer/{livreurId}")
    @Operation(
        summary = "Confirmer la livraison",
        description = "Le livreur confirme que le colis a été remis au destinataire. Passe le statut à LIVRE et envoie un événement Kafka."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Livraison confirmée — événement Kafka envoyé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Livraison introuvable")
    })
    public ResponseEntity<LivraisonResponse> confirmerLivraison(
            @Parameter(description = "ID de la livraison", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID du livreur", required = true, example = "1") @PathVariable Long livreurId) {
        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.confirmerLivraison(id, livreurId));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/livraisons/{id}/echec/{livreurId}
    // -------------------------------------------------------------------------
    @PatchMapping("/api/livraisons/{id}/echec/{livreurId}")
    @Operation(
        summary = "Enregistrer un échec de livraison",
        description = "Le livreur signale qu'il n'a pas pu livrer le colis. Un motif est obligatoire. Passe le statut à ECHEC."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Échec enregistré"),
        @ApiResponse(responseCode = "400", description = "Motif d'échec manquant"),
        @ApiResponse(responseCode = "403", description = "Accès refusé"),
        @ApiResponse(responseCode = "404", description = "Livraison introuvable")
    })
    public ResponseEntity<LivraisonResponse> enregistrerEchec(
            @Parameter(description = "ID de la livraison", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID du livreur", required = true, example = "1") @PathVariable Long livreurId,
            @Valid @RequestBody EchecRequest request) {
        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.enregistrerEchec(id, livreurId, request));
    }


// -------------------------------------------------------------------------
// GET /api/livreurs/me/tournee
// -------------------------------------------------------------------------
@GetMapping("/api/livreurs/me/tournee")
@Operation(summary = "Ma tournée du jour", description = "Retourne la tournée du livreur connecté en utilisant son userId.")
public ResponseEntity<List<LivraisonResponse>> getMaTournee() {
    Long userId = SecurityContext.getCurrentUserId();
    return ResponseEntity.ok(livraisonService.getTourneeJourParUserId(userId));
}

// -------------------------------------------------------------------------
// GET /api/livreurs/me/livraisons
// -------------------------------------------------------------------------
@GetMapping("/api/livreurs/me/livraisons")
@Operation(summary = "Mon historique", description = "Retourne l'historique du livreur connecté.")
public ResponseEntity<List<LivraisonResponse>> getMonHistorique() {
    Long userId = SecurityContext.getCurrentUserId();
    return ResponseEntity.ok(livraisonService.getLivraisonsParUserId(userId));
}
    
    // -------------------------------------------------------------------------
    // Zero Trust RBAC helpers
    // -------------------------------------------------------------------------
    private void verifierAdmin() {
        if (!SecurityContext.isAdmin()) {
            throw new com.livraison.livreur.exception.AccessDeniedException(
                    "Accès réservé aux administrateurs");
        }
    }

    private void verifierLivreurOuAdmin(Long livreurId) {
        if (SecurityContext.isAdmin()) return;
        Long currentUserId = SecurityContext.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(livreurId)) {
            throw new com.livraison.livreur.exception.AccessDeniedException(
                    "Accès refusé : vous ne pouvez agir que sur vos propres livraisons");
        }
    }
}