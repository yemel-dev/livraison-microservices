package com.livraison.livreur.controller;


import com.livraison.livreur.dto.AssignerLivraisonRequest;
import com.livraison.livreur.dto.EchecRequest;
import com.livraison.livreur.dto.LivraisonResponse;
import com.livraison.livreur.security.SecurityContext;
import com.livraison.livreur.service.LivraisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST — Gestion des Livraisons & Tournées
 * Routes :
 *   POST   /api/livraisons                              → Assigner colis         (ADMIN)
 *   GET    /api/livraisons/{id}                         → Détail livraison        (ADMIN, LIVREUR)
 *   GET    /api/livreurs/{livreurId}/tournee            → Tournée du jour         (LIVREUR — sa tournée uniquement)
 *   GET    /api/livreurs/{livreurId}/livraisons         → Historique livraisons   (LIVREUR — ses livraisons)
 *   PATCH  /api/livraisons/{id}/scanner/{livreurId}     → Scan prise en charge    (LIVREUR)
 *   PATCH  /api/livraisons/{id}/transit/{livreurId}     → Mettre en transit       (LIVREUR)
 *   PATCH  /api/livraisons/{id}/confirmer/{livreurId}   → Confirmer livraison     (LIVREUR)
 *   PATCH  /api/livraisons/{id}/echec/{livreurId}       → Enregistrer échec       (LIVREUR)
 */

/**
 *expose les 8 endpoints de livraison répartis sur /api/livraisons et /api/livreurs/{id}/tournee. Les
 *endpoints de modification vérifient que X-User-Id correspond au livreurId dans l'URL.
 */

@RestController
@RequiredArgsConstructor
@Slf4j
public class LivraisonController {

    private final LivraisonService livraisonService;

    //  POST /api/livraisons — Assigner un colis à un livreur (ADMIN) ─
    @PostMapping("/api/livraisons")
    public ResponseEntity<LivraisonResponse> assignerColis(
            @Valid @RequestBody AssignerLivraisonRequest request) {

        verifierAdmin();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(livraisonService.assignerColis(request));
    }

    //  GET /api/livraisons/{id}
    @GetMapping("/api/livraisons/{id}")
    public ResponseEntity<LivraisonResponse> getLivraison(@PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraison(id));
    }

    //  GET /api/livreurs/{livreurId}/tournee — Tournée du jour
    @GetMapping("/api/livreurs/{livreurId}/tournee")
    public ResponseEntity<List<LivraisonResponse>> getTourneeJour(
            @PathVariable Long livreurId) {

        // Zero Trust vérifié dans le service
        return ResponseEntity.ok(livraisonService.getTourneeJour(livreurId));
    }

    //  GET /api/livreurs/{livreurId}/livraisons — Historique complet ─
    @GetMapping("/api/livreurs/{livreurId}/livraisons")
    public ResponseEntity<List<LivraisonResponse>> getLivraisonsLivreur(
            @PathVariable Long livreurId) {

        return ResponseEntity.ok(livraisonService.getLivraisonsLivreur(livreurId));
    }

    //  PATCH /api/livraisons/{id}/scanner/{livreurId} — Scan prise en charge ─
    @PatchMapping("/api/livraisons/{id}/scanner/{livreurId}")
    public ResponseEntity<LivraisonResponse> scanner(
            @PathVariable Long id,
            @PathVariable Long livreurId) {

        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.scannerPriseEnCharge(id, livreurId));
    }

    //  PATCH /api/livraisons/{id}/transit/{livreurId} — En transit
    @PatchMapping("/api/livraisons/{id}/transit/{livreurId}")
    public ResponseEntity<LivraisonResponse> mettreEnTransit(
            @PathVariable Long id,
            @PathVariable Long livreurId) {

        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.mettreEnTransit(id, livreurId));
    }

    //  PATCH /api/livraisons/{id}/confirmer/{livreurId} — Confirmer livraison ─
    @PatchMapping("/api/livraisons/{id}/confirmer/{livreurId}")
    public ResponseEntity<LivraisonResponse> confirmerLivraison(
            @PathVariable Long id,
            @PathVariable Long livreurId) {

        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.confirmerLivraison(id, livreurId));
    }

    //  PATCH /api/livraisons/{id}/echec/{livreurId} — Enregistrer échec
    @PatchMapping("/api/livraisons/{id}/echec/{livreurId}")
    public ResponseEntity<LivraisonResponse> enregistrerEchec(
            @PathVariable Long id,
            @PathVariable Long livreurId,
            @Valid @RequestBody EchecRequest request) {

        verifierLivreurOuAdmin(livreurId);
        return ResponseEntity.ok(livraisonService.enregistrerEchec(id, livreurId, request));
    }

    //  Zero Trust RBAC helpers ─

    private void verifierAdmin() {
        if (!SecurityContext.isAdmin()) {
            throw new com.livraison.livreur.exception.AccessDeniedException(
                    "Accès réservé aux administrateurs");
        }
    }

    private void verifierLivreurOuAdmin(Long livreurId) {
        if (SecurityContext.isAdmin()) return;
        // Un livreur ne peut agir que sur ses propres livraisons
        Long currentUserId = SecurityContext.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(livreurId)) {
            throw new com.livraison.livreur.exception.AccessDeniedException(
                    "Accès refusé : vous ne pouvez agir que sur vos propres livraisons");
        }
    }
}
