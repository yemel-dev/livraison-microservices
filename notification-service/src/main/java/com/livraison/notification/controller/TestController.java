package com.livraison.notification.controller;

import com.livraison.notification.event.ColisCreatedEvent;
import com.livraison.notification.event.ColisStatusChangedEvent;
import com.livraison.notification.event.LivraisonDoneEvent;
import com.livraison.notification.service.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(
    name = "Test des événements Kafka",
    description = "Endpoints de test pour simuler les événements des autres microservices"
)
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestController(KafkaProducerService kafkaProducerService, ObjectMapper objectMapper) {
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────
    // ENDPOINT 1 — Simuler la création d'un colis
    // POST /api/test/colis-created
    // ─────────────────────────────────────────────────
    @PostMapping("/colis-created")
    @Operation(
        summary = "Simuler la création d'un colis",
        description = "Publie un événement sur le topic Kafka 'colis.created'. " +
                      "Le KafkaConsumer simule l'envoi de 2 emails (expéditeur + destinataire).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = ColisCreatedEvent.class),
                examples = @ExampleObject(
                    name = "Exemple",
                    value = "{\"colisId\":1," +
                            "\"numeroSuivi\":\"COL-20260529-A3F7K\"," +
                            "\"expediteurNom\":\"Alice Dupont\"," +
                            "\"expediteurEmail\":\"alice@gmail.com\"," +
                            "\"destinataireNom\":\"Bob Martin\"," +
                            "\"destinataireEmail\":\"bob@gmail.com\"," +
                            "\"optionService\":\"EXPRESS\"," +
                            "\"delaiLivraisonJours\":2}"
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Événement publié avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la publication")
        }
    )
    public ResponseEntity<Map<String, String>> simulerColisCreated(
            @RequestBody ColisCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaProducerService.publierColisCreated(json);
            log.info("✅ Événement colis.created publié pour colisId={}", event.getColisId());
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Événement publié sur le topic colis.created",
                "colisId", String.valueOf(event.getColisId())
            ));
        } catch (Exception e) {
            log.error("❌ Erreur publication colis.created : {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────────
    // ENDPOINT 2 — Simuler un changement de statut
    // POST /api/test/colis-status-changed
    // ─────────────────────────────────────────────────
    @PostMapping("/colis-status-changed")
    @Operation(
        summary = "Simuler un changement de statut de colis",
        description = "Publie un événement sur le topic Kafka 'colis.status_changed'.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = ColisStatusChangedEvent.class),
                examples = @ExampleObject(
                    name = "Exemple",
                    value = "{\"colisId\":1," +
                            "\"numeroSuivi\":\"COL-20260529-A3F7K\"," +
                            "\"ancienStatut\":\"EN_ATTENTE\"," +
                            "\"nouveauStatut\":\"ENLEVE\"," +
                            "\"destinataireEmail\":\"bob@gmail.com\"," +
                            "\"destinataireNom\":\"Bob Martin\"}"
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Événement publié avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la publication")
        }
    )
    public ResponseEntity<Map<String, String>> simulerColisStatusChanged(
            @RequestBody ColisStatusChangedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaProducerService.publierColisStatusChanged(json);
            log.info("✅ Événement colis.status_changed publié pour colisId={}", event.getColisId());
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Événement publié sur le topic colis.status_changed",
                "colisId", String.valueOf(event.getColisId()),
                "nouveauStatut", event.getNouveauStatut()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur publication colis.status_changed : {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────────
    // ENDPOINT 3 — Simuler une livraison terminée
    // POST /api/test/livraison-done
    // ─────────────────────────────────────────────────
    @PostMapping("/livraison-done")
    @Operation(
        summary = "Simuler une livraison terminée",
        description = "Publie un événement sur le topic Kafka 'livraison.done'.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = LivraisonDoneEvent.class),
                examples = @ExampleObject(
                    name = "Exemple",
                    value = "{\"numeroSuivi\":\"COL-20260529-A3F7K\"," +
                            "\"livreurId\":1," +
                            "\"livreurNom\":\"Paul Dupont\"," +
                            "\"dateLivraison\":\"2026-05-29T18:00:00\"," +
                            "\"eventType\":\"LIVRAISON_CONFIRMEE\"}"
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Événement publié avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la publication")
        }
    )
    public ResponseEntity<Map<String, String>> simulerLivraisonDone(
            @RequestBody LivraisonDoneEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaProducerService.publierLivraisonDone(json);
            log.info("✅ Événement livraison.done publié pour numeroSuivi={}", event.getNumeroSuivi());
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Événement publié sur le topic livraison.done",
                "numeroSuivi", event.getNumeroSuivi(),
                "livreurNom", event.getLivreurNom()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur publication livraison.done : {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }
}