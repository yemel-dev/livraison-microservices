package com.livraison.livreur.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.livraison-done}")
    private String livraisonDoneTopic;

    @Value("${kafka.topics.colis-status-changed}")
    private String colisStatusChangedTopic;

    public void publierLivraisonDone(LivraisonDoneEvent event) {
        try {
            log.info("[KAFKA] Publication livraison.done → numéroSuivi={}", event.getNumeroSuivi());
            kafkaTemplate.send(livraisonDoneTopic, event.getNumeroSuivi(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("[KAFKA] Échec envoi livraison.done (Kafka indisponible) : {}", ex.getMessage());
                        } else {
                            log.info("[KAFKA] livraison.done envoyé avec succès");
                        }
                    });
        } catch (Exception e) {
            log.warn("[KAFKA] Kafka indisponible, événement ignoré : {}", e.getMessage());
        }
    }

    public void publierColisStatusChanged(ColisStatusChangedEvent event) {
        try {
            log.info("[KAFKA] Publication colis.status_changed → {}→{}",
                    event.getAncienStatut(), event.getNouveauStatut());
            kafkaTemplate.send(colisStatusChangedTopic, event.getNumeroSuivi(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("[KAFKA] Échec envoi colis.status_changed (Kafka indisponible) : {}", ex.getMessage());
                        } else {
                            log.info("[KAFKA] colis.status_changed envoyé avec succès");
                        }
                    });
        } catch (Exception e) {
            log.warn("[KAFKA] Kafka indisponible, événement ignoré : {}", e.getMessage());
        }
    }
}