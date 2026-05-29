package com.livraison.notification.kafka;

import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import com.livraison.notification.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.notification.event.ColisCreatedEvent;
import com.livraison.notification.event.ColisStatusChangedEvent;
import com.livraison.notification.event.LivraisonDoneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaConsumer(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────
    // LISTENER 1 — topic "colis.created" (publié par colis-service)
    // ─────────────────────────────────────────────────────
    @KafkaListener(topics = "colis.created", groupId = "notification-group")
    public void ecouterColisCreated(String message) {
        log.info("📨 Message reçu sur colis.created : {}", message);
        try {
            ColisCreatedEvent event = objectMapper.readValue(message, ColisCreatedEvent.class);

            // Email à l'expéditeur
            emailService.envoyerConfirmationExpediteur(
                event.getExpediteurEmail(),
                String.valueOf(event.getColisId()),
                event.getNumeroSuivi()
            );

            // Email au destinataire
            emailService.envoyerAvisExpeditionDestinataire(
                event.getDestinataireEmail(),
                String.valueOf(event.getColisId()),
                event.getNumeroSuivi()
            );

        } catch (Exception e) {
            log.error("❌ Erreur traitement colis.created : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // LISTENER 2 — topic "colis.status_changed" (publié par colis-service)
    // ─────────────────────────────────────────────────────
    @KafkaListener(topics = "colis.status_changed", groupId = "notification-group")
    public void ecouterColisStatusChanged(String message) {
        log.info("📨 Message reçu sur colis.status_changed : {}", message);
        try {
            ColisStatusChangedEvent event = objectMapper.readValue(message, ColisStatusChangedEvent.class);

            // Email au destinataire (seul email disponible dans cet event)
            emailService.envoyerNotificationStatut(
                event.getDestinataireEmail(),
                String.valueOf(event.getColisId()),
                event.getAncienStatut(),
                event.getNouveauStatut()
            );

        } catch (Exception e) {
            log.error("❌ Erreur traitement colis.status_changed : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // LISTENER 3 — topic "livraison.done" (publié par livreur-service)
    // ─────────────────────────────────────────────────────
    @KafkaListener(topics = "livraison.done", groupId = "notification-group")
    public void ecouterLivraisonDone(String message) {
        log.info("📨 Message reçu sur livraison.done : {}", message);
        try {
            LivraisonDoneEvent event = objectMapper.readValue(message, LivraisonDoneEvent.class);

            // Confirmation finale au destinataire via numeroSuivi
            emailService.envoyerConfirmationLivraisonDestinataire(
                event.getNumeroSuivi(),
                event.getNumeroSuivi(),
                event.getLivreurNom(),
                String.valueOf(event.getDateLivraison())
            );

        } catch (Exception e) {
            log.error("❌ Erreur traitement livraison.done : {}", e.getMessage());
        }
    }
}