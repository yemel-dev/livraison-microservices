package com.livraison.livreur.controller;


import com.livraison.livreur.dto.CreateLivreurRequest;
import com.livraison.livreur.dto.LivreurResponse;
import com.livraison.livreur.dto.UpdateLivreurRequest;
import com.livraison.livreur.security.SecurityContext;
import com.livraison.livreur.service.LivreurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST — Gestion des profils Livreurs
 * Routes :
 *   POST   /api/livreurs              → Créer un livreur          (ADMIN)
 *   GET    /api/livreurs              → Liste des livreurs actifs  (ADMIN, LIVREUR)
 *   GET    /api/livreurs/tous         → Tous les livreurs          (ADMIN)
 *   GET    /api/livreurs/{id}         → Détail d'un livreur        (ADMIN, LIVREUR)
 *   PUT    /api/livreurs/{id}         → Modifier un livreur        (ADMIN)
 *   PATCH  /api/livreurs/{id}/toggle  → Activer/Désactiver         (ADMIN)
 *   DELETE /api/livreurs/{id}         → Désactiver (soft delete)   (ADMIN)
 */


/**
 * @RequestMapping("/api/livreurs") — expose les 7 endpoints CRUD.
 * Chaque méthode qui nécessite le rôle ADMIN appelle verifierAdmin() qui lit le header via SecurityContext
 */
@RestController
@RequestMapping("/api/livreurs")
@RequiredArgsConstructor
@Slf4j
public class LivreurController {

    private final LivreurService livreurService;

    //  POST /api/livreurs
    @PostMapping
    public ResponseEntity<LivreurResponse> creerLivreur(
            @Valid @RequestBody CreateLivreurRequest request) {

        verifierAdmin();
        LivreurResponse response = livreurService.creerLivreur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //  GET /api/livreurs ─
    @GetMapping
    public ResponseEntity<List<LivreurResponse>> listerLivreursActifs() {
        return ResponseEntity.ok(livreurService.listerLivreursActifs());
    }

    //  GET /api/livreurs/tous
    @GetMapping("/tous")
    public ResponseEntity<List<LivreurResponse>> listerTousLivreurs() {
        verifierAdmin();
        return ResponseEntity.ok(livreurService.listerTousLivreurs());
    }

    //  GET /api/livreurs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<LivreurResponse> getLivreur(@PathVariable Long id) {
        return ResponseEntity.ok(livreurService.getLivreurById(id));
    }

    //  PUT /api/livreurs/{id}
    @PutMapping("/{id}")
    public ResponseEntity<LivreurResponse> mettreAJour(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLivreurRequest request) {

        verifierAdmin();
        return ResponseEntity.ok(livreurService.mettreAJourLivreur(id, request));
    }

    //  PATCH /api/livreurs/{id}/toggle
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<LivreurResponse> toggleActif(@PathVariable Long id) {
        verifierAdmin();
        return ResponseEntity.ok(livreurService.toggleActif(id));
    }

    //  DELETE /api/livreurs/{id} ─
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        verifierAdmin();
        livreurService.supprimerLivreur(id);
        return ResponseEntity.noContent().build();
    }

    //  Zero Trust RBAC ─
    private void verifierAdmin() {
        if (!SecurityContext.isAdmin()) {
            throw new com.livraison.livreur.exception.AccessDeniedException(
                    "Accès réservé aux administrateurs");
        }
    }

}
