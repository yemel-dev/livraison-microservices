package com.livraison.notification.kafka;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import com.livraison.notification.service.EmailService;

/**
 * Test d'intégration — utilise un vrai Kafka en mémoire. Pas besoin de Docker.
 * Spring démarre un Kafka embarqué.
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"colis-created", "colis-status-changed", "livraison-done"}
)
public class KafkaConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // MockBean remplace le vrai EmailService par un faux
    // géré par Spring
    @MockBean
    private EmailService emailService;

    @Test
    void testFluxComplet_colisCreated() throws Exception {

        String message = """
            {
                "colisId": "COL-001",
                "expediteurEmail": "jean@gmail.com",
                "destinataireEmail": "marie@gmail.com",
                "description": "Telephone Samsung",
                "statut": "CREE"
            }
            """;

        // Publie sur le vrai Kafka embarqué
        kafkaTemplate.send("colis-created", message);

        // Attend 3 secondes que le consumer traite le message
        TimeUnit.SECONDS.sleep(10);

        // Vérifie que EmailService a été appelé
        verify(emailService, times(1)).envoyerConfirmationExpediteur(
                "jean@gmail.com", "COL-001", "Telephone Samsung"
        );
        verify(emailService, times(1)).envoyerAvisExpeditionDestinataire(
                "marie@gmail.com", "COL-001", "Telephone Samsung"
        );
    }
}
