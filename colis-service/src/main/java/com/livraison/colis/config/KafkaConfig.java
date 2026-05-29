package com.livraison.colis.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration du producteur Kafka pour le colis-service.
 *
 * Le colis-service est UNIQUEMENT producteur (il publie des événements).
 * Il ne consomme aucun topic.
 *
 * Sérialisation :
 * - Clé   : String  (le numéro de suivi)
 * - Valeur : JSON   (les objets ColisCreatedEvent / ColisStatusChangedEvent)
 *
 * Utiliser le numéro de suivi comme clé garantit que tous les événements
 * d'un même colis vont dans la même partition Kafka → ordre préservé.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Factory qui crée les instances de producteur Kafka.
     * Configurée avec les propriétés de connexion et de sérialisation.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Adresse du broker Kafka
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Sérialisation de la clé en String
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Sérialisation de la valeur en JSON
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Ne pas inclure le type Java dans le header du message Kafka
        // → évite les problèmes de désérialisation côté consommateur si les packages diffèrent
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Idempotence : évite les doublons en cas de retry réseau
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Attendre l'acquittement de tous les replicas avant de confirmer l'envoi
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // Nombre de tentatives automatiques en cas d'erreur transitoire
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate : bean Spring injecté dans ColisKafkaProducer.
     * C'est le point d'entrée pour envoyer des messages vers Kafka.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}