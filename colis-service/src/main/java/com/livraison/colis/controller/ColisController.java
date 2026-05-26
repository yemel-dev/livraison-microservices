package com.livraison.colis.controller;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.dto.UpdateStatutDTO;
import com.livraison.colis.service.ColisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour les opérations sur les colis.
 *
 * Rôle unique : recevoir la requête HTTP, extraire les headers,
 * déléguer au service, retourner la ResponseEntity.
 * AUCUNE logique métier ici.
 *
 * Headers attendus (injectés par l'api-gateway, Zero Trust) :
 * - X-User-Id   : Long  → ID de l'utilisateur authentifié
 * - X-User-Role : String → rôle (ROLE_CLIENT, ROLE_LIVREUR, ROLE_ADMIN)
 */
@RestController
@RequestMapping("/api/colis")
@RequiredArgsConstructor
@Slf4j
public class ColisController {

    private final ColisService colisService;

    // ─── POST /api/colis ─────────────────────────────────────────────────────

    /**
     * Créer un nouveau colis.
     * Accès : CLIENT, ADMIN
     *
     * @param dto    corps de la requête validé par @Valid
     * @param userId extrait du header X-User-Id
     * @return 201 Created + le colis créé
     */
    @PostMapping
    public ResponseEntity<ColisResponseDTO> creerColis(
            @Valid @RequestBody ColisRequestDTO dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("POST /api/colis - userId={}, role={}", userId, role);
        ColisResponseDTO response = colisService.creerColis(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── GET /api/colis ──────────────────────────────────────────────────────

    /**
     * Lister les colis.
     * CLIENT → uniquement ses colis.
     * ADMIN  → tous les colis.
     *
     * @return 200 OK + liste des colis
     */
    @GetMapping
    public ResponseEntity<List<ColisResponseDTO>> getAllColis(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("GET /api/colis - userId={}, role={}", userId, role);
        List<ColisResponseDTO> colis = colisService.getAllColis(userId, role);
        return ResponseEntity.ok(colis);
    }

    // ─── GET /api/colis/{id} ─────────────────────────────────────────────────

    /**
     * Récupérer un colis par son ID.
     * CLIENT → seulement si c'est le sien.
     * ADMIN  → tous.
     *
     * @return 200 OK + le colis, ou 404 si inexistant, ou 403 si non autorisé
     */
    @GetMapping("/{id}")
    public ResponseEntity<ColisResponseDTO> getColisById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("GET /api/colis/{} - userId={}, role={}", id, userId, role);
        ColisResponseDTO colis = colisService.getColisById(id, userId, role);
        return ResponseEntity.ok(colis);
    }

    // ─── GET /api/colis/suivi/{numeroSuivi} ──────────────────────────────────

    /**
     * Rechercher un colis par numéro de suivi.
     * Endpoint de suivi — accessible à tout utilisateur authentifié.
     *
     * @return 200 OK + le colis, ou 404 si inexistant
     */
    @GetMapping("/suivi/{numeroSuivi}")
    public ResponseEntity<ColisResponseDTO> getColisByNumeroSuivi(
            @PathVariable String numeroSuivi,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("GET /api/colis/suivi/{} - userId={}", numeroSuivi, userId);
        ColisResponseDTO colis = colisService.getColisByNumeroSuivi(numeroSuivi);
        return ResponseEntity.ok(colis);
    }

    // ─── PUT /api/colis/{id} ─────────────────────────────────────────────────

    /**
     * Mettre à jour les informations d'un colis (seulement si EN_ATTENTE).
     * CLIENT (owner) ou ADMIN.
     *
     * @return 200 OK + le colis mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<ColisResponseDTO> updateColis(
            @PathVariable Long id,
            @Valid @RequestBody ColisRequestDTO dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("PUT /api/colis/{} - userId={}, role={}", id, userId, role);
        ColisResponseDTO updated = colisService.updateColis(id, dto, userId, role);
        return ResponseEntity.ok(updated);
    }

    // ─── PATCH /api/colis/{id}/statut ────────────────────────────────────────

    /**
     * Changer le statut d'un colis.
     * LIVREUR, ADMIN uniquement.
     *
     * Body JSON attendu :
     * { "nouveauStatut": "ENLEVE" }
     *
     * @return 200 OK + le colis avec son nouveau statut
     */
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ColisResponseDTO> updateStatut(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatutDTO updateStatutDTO,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("PATCH /api/colis/{}/statut → {} - userId={}, role={}",
                id, updateStatutDTO.getNouveauStatut(), userId, role);

        ColisResponseDTO updated = colisService.updateStatut(
                id, updateStatutDTO.getNouveauStatut(), userId, role);

        return ResponseEntity.ok(updated);
    }

    // ─── DELETE /api/colis/{id} ──────────────────────────────────────────────

    /**
     * Supprimer un colis (seulement si EN_ATTENTE).
     * ADMIN uniquement.
     *
     * @return 204 No Content si succès
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColis(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        log.info("DELETE /api/colis/{} - userId={}, role={}", id, userId, role);
        colisService.deleteColis(id, userId, role);
        return ResponseEntity.noContent().build();
    }
}