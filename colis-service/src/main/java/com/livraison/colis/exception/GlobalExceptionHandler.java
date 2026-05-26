package com.livraison.colis.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions.
 * Intercepte toutes les exceptions lancées par les controllers et services,
 * et retourne des réponses JSON structurées et cohérentes.
 *
 * Principe : le client ne doit JAMAIS voir une stack trace Java.
 * Il reçoit toujours un JSON propre avec un code HTTP approprié.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── 404 — Colis introuvable ──────────────────────────────────────────────

    @ExceptionHandler(ColisNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleColisNotFound(ColisNotFoundException ex) {
        log.warn("Colis introuvable : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 404);
        body.put("error", "Colis introuvable");
        body.put("message", ex.getMessage());

        if (ex.getId() != null) {
            body.put("id", ex.getId());
        }
        if (ex.getNumeroSuivi() != null) {
            body.put("numeroSuivi", ex.getNumeroSuivi());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ─── 400 — Transition de statut invalide ─────────────────────────────────

    @ExceptionHandler(InvalidStatutTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidStatutTransitionException ex) {
        log.warn("Transition invalide : {} → {}", ex.getStatutActuel(), ex.getStatutCible());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Transition de statut invalide");
        body.put("message", ex.getMessage());
        body.put("statutActuel", ex.getStatutActuel());
        body.put("statutCible", ex.getStatutCible());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ─── 400 — Validation des DTOs (@Valid échoue) ───────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collecter tous les messages d'erreur par champ
        Map<String, String> erreurs = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valeur invalide",
                        // En cas de champs dupliqués, garder le premier message
                        (msg1, msg2) -> msg1
                ));

        log.warn("Erreurs de validation : {}", erreurs);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Données invalides");
        body.put("erreurs", erreurs);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ─── 400 — État métier invalide (ex: suppression d'un colis non EN_ATTENTE) ─

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("État métier invalide : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Opération non autorisée");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ─── 401 — Header X-User-Id manquant ─────────────────────────────────────

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Header manquant : {}", ex.getHeaderName());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 401);
        body.put("error", "Non authentifié");
        body.put("message", "Header obligatoire manquant : " + ex.getHeaderName());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ─── 403 — Accès refusé ───────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Accès refusé : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 403);
        body.put("error", "Accès non autorisé");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ─── 500 — Erreur inattendue ──────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log complet avec stack trace pour le débogage côté serveur
        log.error("Erreur interne non gérée", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 500);
        body.put("error", "Erreur interne du serveur");
        // Ne jamais exposer le message technique au client en prod
        body.put("message", "Une erreur inattendue s'est produite. Veuillez réessayer.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}