package com.livraison.user.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publierUserCreated(UserCreatedEvent event) {
        try {
            kafkaTemplate.send("user.created", event.getEmail(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[KAFKA] Échec envoi user.created : {}", ex.getMessage());
                    } else {
                        log.info("[KAFKA] ✅ user.created envoyé userId={} role={}",
                                event.getUserId(), event.getRole());
                    }
                });
        } catch (Exception e) {
            log.warn("[KAFKA] Kafka indisponible, inscription continue : {}", e.getMessage());
        }
    }
}