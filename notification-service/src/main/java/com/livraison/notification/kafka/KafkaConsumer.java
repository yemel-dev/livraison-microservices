package com.livraison.notification.kafka;

// Permet à Spring de détecter cette classe automatiquement
import org.springframework.stereotype.Component;

// L'annotation magique qui écoute les topics Kafka
import org.springframework.kafka.annotation.KafkaListener;

// Notre service d'envoi d'emails
import com.livraison.notification.service.EmailService;

// Jackson : convertit le JSON reçu en objet Java
import com.fasterxml.jackson.databind.ObjectMapper;

// Nos 3 events
import com.livraison.notification.event.ColisCreatedEvent;
import com.livraison.notification.event.ColisStatusChangedEvent;
import com.livraison.notification.event.LivraisonDoneEvent;

// Logger pour afficher ce qui se passe
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Injection de dépendances via constructeur
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Ce composant écoute en permanence les 3 topics Kafka.
 * Dès qu'un message arrive, la méthode correspondante
 * est appelée automatiquement par Spring.
 */
@Component
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // EmailService : pour simuler les envois d'emails
    private final EmailService emailService;

    // ObjectMapper : l'outil Jackson qui transforme
    // le JSON (String) en objet Java
    private final ObjectMapper objectMapper;

    // Spring injecte automatiquement EmailService
    // et ObjectMapper grâce à @Autowired
    @Autowired
    public KafkaConsumer(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────
    // LISTENER 1 — Écoute le topic "colis.created"
    //
    // @KafkaListener dit à Spring :
    // "Dès qu'un message arrive sur ce topic,
    //  appelle cette méthode automatiquement"
    //
    // groupId : identifie notre service auprès de Kafka
    // ─────────────────────────────────────────────────────
    @KafkaListener(topics = "colis-created", groupId = "notification-group")
    public void ecouterColisCreated(String message) {

        log.info("📨 Message reçu sur colis.created : {}", message);

        try {
            // On convertit le JSON reçu en objet Java
            // Ex: '{"colisId":"COL-001",...}' → ColisCreatedEvent
            ColisCreatedEvent event = objectMapper.readValue(message, ColisCreatedEvent.class);

            // Email 1 → confirmation à l'expéditeur
            emailService.envoyerConfirmationExpediteur(
                event.getExpediteurEmail(),
                event.getColisId(),
                event.getDescription()
            );

            // Email 2 → avis d'expédition au destinataire
            emailService.envoyerAvisExpeditionDestinataire(
                event.getDestinataireEmail(),
                event.getColisId(),
                event.getDescription()
            );

        } catch (Exception e) {
            // Si le message est mal formé on log l'erreur
            // Kafka ne rejoue pas ce message — on le signale
            log.error("❌ Erreur traitement colis.created : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // LISTENER 2 — Écoute le topic "colis.status_changed"
    // ─────────────────────────────────────────────────────
    @KafkaListener(topics = "colis-status-changed", groupId = "notification-group")
    public void ecouterColisStatusChanged(String message) {

        log.info("📨 Message reçu sur colis.status_changed : {}", message);

        try {
            ColisStatusChangedEvent event = objectMapper.readValue(message, ColisStatusChangedEvent.class);

            // Email à l'expéditeur
            emailService.envoyerNotificationStatut(
                event.getExpediteurEmail(),
                event.getColisId(),
                event.getAncienStatut(),
                event.getNouveauStatut()
            );

            // Email au destinataire
            emailService.envoyerNotificationStatut(
                event.getDestinataireEmail(),
                event.getColisId(),
                event.getAncienStatut(),
                event.getNouveauStatut()
            );

        } catch (Exception e) {
            log.error("❌ Erreur traitement colis.status_changed : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // LISTENER 3 — Écoute le topic "livraison.done"
    // ─────────────────────────────────────────────────────
    @KafkaListener(topics = "livraison-done", groupId = "notification-group")
    public void ecouterLivraisonDone(String message) {

        log.info("📨 Message reçu sur livraison.done : {}", message);

        try {
            LivraisonDoneEvent event = objectMapper.readValue(message, LivraisonDoneEvent.class);

            // Email de confirmation finale à l'expéditeur
            emailService.envoyerConfirmationLivraisonExpediteur(
                event.getExpediteurEmail(),
                event.getColisId(),
                event.getLivreurNom(),
                event.getDateLivraison()
            );

            // Email de confirmation finale au destinataire
            emailService.envoyerConfirmationLivraisonDestinataire(
                event.getDestinataireEmail(),
                event.getColisId(),
                event.getLivreurNom(),
                event.getDateLivraison()
            );

        } catch (Exception e) {
            log.error("❌ Erreur traitement livraison.done : {}", e.getMessage());
        }
    }
}