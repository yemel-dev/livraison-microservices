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
 * Topics alignés avec ceux publiés par colis-service et livreur-service.
 */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publierColisCreated(String messageJson) {
        log.info("📤 Publication sur colis.created : {}", messageJson);
        kafkaTemplate.send("colis.created", messageJson);
    }

    public void publierColisStatusChanged(String messageJson) {
        log.info("📤 Publication sur colis.status_changed : {}", messageJson);
        kafkaTemplate.send("colis.status_changed", messageJson);
    }

    public void publierLivraisonDone(String messageJson) {
        log.info("📤 Publication sur livraison.done : {}", messageJson);
        kafkaTemplate.send("livraison.done", messageJson);
    }
}