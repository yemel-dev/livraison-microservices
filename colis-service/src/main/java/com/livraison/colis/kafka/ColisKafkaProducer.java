package com.livraison.colis.kafka;

import com.livraison.colis.kafka.event.ColisCreatedEvent;
import com.livraison.colis.kafka.event.ColisStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Producteur Kafka du colis-service.
 *
 * Publie deux types d'événements :
 * - colis.created        → après création réussie en BDD
 * - colis.status_changed → après chaque changement de statut
 *
 * Règle fondamentale : publier APRÈS le commit BDD.
 * Si la BDD échoue, l'événement ne doit PAS être publié.
 * C'est garanti par l'ordre d'appel dans ColisServiceImpl :
 *   1. colisRepository.save()  ← transaction BDD
 *   2. kafkaProducer.publish() ← seulement si save() a réussi
 *
 * Gestion des erreurs :
 * - On logge l'erreur Kafka mais on ne fait PAS échouer la requête HTTP.
 * - La livraison de la notification est best-effort.
 * - En production, envisager un outbox pattern pour garantir la livraison.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColisKafkaProducer {

    private static final String TOPIC_COLIS_CREATED        = "colis.created";
    private static final String TOPIC_COLIS_STATUS_CHANGED = "colis.status_changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publie un événement de création de colis sur le topic "colis.created".
     *
     * La clé du message est le numéro de suivi → garantit que tous les
     * événements d'un même colis vont dans la même partition (ordre préservé).
     *
     * @param event l'événement à publier
     */
    public void publishColisCreated(ColisCreatedEvent event) {
        log.info("Publication Kafka [{}] : colisId={}, numeroSuivi={}",
                TOPIC_COLIS_CREATED, event.getColisId(), event.getNumeroSuivi());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_COLIS_CREATED, event.getNumeroSuivi(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // Erreur Kafka : on logge mais on ne propage pas l'exception
                // La requête HTTP a déjà retourné 201 au client
                log.error("Échec publication Kafka [{}] colisId={} : {}",
                        TOPIC_COLIS_CREATED, event.getColisId(), ex.getMessage());
            } else {
                log.debug("Message Kafka envoyé [{}] partition={} offset={}",
                        TOPIC_COLIS_CREATED,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publie un événement de changement de statut sur le topic "colis.status_changed".
     *
     * @param event l'événement à publier
     */
    public void publishStatusChanged(ColisStatusChangedEvent event) {
        log.info("Publication Kafka [{}] : colisId={}, {} → {}",
                TOPIC_COLIS_STATUS_CHANGED,
                event.getColisId(),
                event.getAncienStatut(),
                event.getNouveauStatut());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_COLIS_STATUS_CHANGED, event.getNumeroSuivi(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Échec publication Kafka [{}] colisId={} : {}",
                        TOPIC_COLIS_STATUS_CHANGED, event.getColisId(), ex.getMessage());
            } else {
                log.debug("Message Kafka envoyé [{}] partition={} offset={}",
                        TOPIC_COLIS_STATUS_CHANGED,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}