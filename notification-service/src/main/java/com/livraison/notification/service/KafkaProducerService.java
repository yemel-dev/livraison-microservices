package com.livraison.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service qui publie des messages sur les topics Kafka.
 * Utilisé uniquement par le TestController pour simuler
 * les événements des autres microservices.
 */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    // KafkaTemplate : outil Spring pour envoyer des messages Kafka
    // Spring le crée automatiquement grâce à la config dans application.yml
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ─────────────────────────────────────────────────
    // Publie un message sur le topic "colis-created"
    // ─────────────────────────────────────────────────
    public void publierColisCreated(String messageJson) {
        log.info("📤 Publication sur colis-created : {}", messageJson);
        kafkaTemplate.send("colis-created", messageJson);
    }

    // ─────────────────────────────────────────────────
    // Publie un message sur le topic "colis-status-changed"
    // ─────────────────────────────────────────────────
    public void publierColisStatusChanged(String messageJson) {
        log.info("📤 Publication sur colis-status-changed : {}", messageJson);
        kafkaTemplate.send("colis-status-changed", messageJson);
    }

    // ─────────────────────────────────────────────────
    // Publie un message sur le topic "livraison-done"
    // ─────────────────────────────────────────────────
    public void publierLivraisonDone(String messageJson) {
        log.info("📤 Publication sur livraison-done : {}", messageJson);
        kafkaTemplate.send("livraison-done", messageJson);
    }
}