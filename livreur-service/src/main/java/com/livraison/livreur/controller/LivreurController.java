package com.livraison.livreur.controller;

import com.livraison.livreur.dto.CreateLivreurRequest;
import com.livraison.livreur.dto.LivreurResponse;
import com.livraison.livreur.dto.UpdateLivreurRequest;
import com.livraison.livreur.security.SecurityContext;
import com.livraison.livreur.service.LivreurService;
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
@RequestMapping("/api/livreurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Livreurs", description = "Gestion des profils livreurs — CRUD complet réservé aux ADMIN")
public class LivreurController {

    private final LivreurService livreurService;

    // -------------------------------------------------------------------------
    // POST /api/livreurs
    // -------------------------------------------------------------------------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un livreur", description = "Crée un nouveau profil livreur. Réservé aux ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Livreur créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides (nom, téléphone, véhicule manquants)"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    public ResponseEntity<LivreurResponse> creerLivreur(
            @Valid @RequestBody CreateLivreurRequest request) {
        verifierAdmin();
        LivreurResponse response = livreurService.creerLivreur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/livreurs
    // -------------------------------------------------------------------------
    @GetMapping
    @Operation(summary = "Liste des livreurs actifs", description = "Retourne uniquement les livreurs avec actif=true.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — token JWT manquant")
    })
    public ResponseEntity<List<LivreurResponse>> listerLivreursActifs() {
        return ResponseEntity.ok(livreurService.listerLivreursActifs());
    }

    // -------------------------------------------------------------------------
    // GET /api/livreurs/tous
    // -------------------------------------------------------------------------
    @GetMapping("/tous")
    @Operation(summary = "Tous les livreurs", description = "Retourne tous les livreurs (actifs et inactifs). Réservé aux ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste complète retournée"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    public ResponseEntity<List<LivreurResponse>> listerTousLivreurs() {
        verifierAdmin();
        return ResponseEntity.ok(livreurService.listerTousLivreurs());
    }

    // -------------------------------------------------------------------------
    // GET /api/livreurs/{id}
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un livreur", description = "Retourne les informations d'un livreur par son ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Livreur trouvé"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<LivreurResponse> getLivreur(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(livreurService.getLivreurById(id));
    }

    // -------------------------------------------------------------------------
    // PUT /api/livreurs/{id}
    // -------------------------------------------------------------------------
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un livreur", description = "Met à jour les informations d'un livreur. Réservé aux ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Livreur mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    public ResponseEntity<LivreurResponse> mettreAJour(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateLivreurRequest request) {
        verifierAdmin();
        return ResponseEntity.ok(livreurService.mettreAJourLivreur(id, request));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/livreurs/{id}/toggle
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activer / Désactiver un livreur", description = "Bascule le statut actif du livreur. Réservé aux ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut basculé"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    public ResponseEntity<LivreurResponse> toggleActif(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id) {
        verifierAdmin();
        return ResponseEntity.ok(livreurService.toggleActif(id));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/livreurs/{id}
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un livreur (soft delete)", description = "Désactive le livreur sans le supprimer de la base. Réservé aux ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Livreur désactivé"),
        @ApiResponse(responseCode = "404", description = "Livreur introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis")
    })
    public ResponseEntity<Void> supprimer(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id) {
        verifierAdmin();
        livreurService.supprimerLivreur(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Zero Trust RBAC
    // -------------------------------------------------------------------------
    private void verifierAdmin() {
        if (!SecurityContext.isAdmin()) {
            throw new com.livraison.livreur.exception.AccessDeniedException(
                    "Accès réservé aux administrateurs");
        }
    }
}