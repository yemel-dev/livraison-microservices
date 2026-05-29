package com.livraison.notification.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.notification.service.EmailService;

/**
 * Test unitaire de KafkaConsumer.
 * On utilise Mockito pour simuler EmailService
 * sans avoir besoin de Kafka ni de Spring.
 *
 * @Mock = crée un faux objet qui enregistre les appels
 * verify() = vérifie qu'une méthode a bien été appelée
 */
@ExtendWith(MockitoExtension.class)
public class KafkaConsumerTest {

    // @Mock crée un faux EmailService
    // On peut vérifier quelles méthodes ont été appelées
    @Mock
    private EmailService emailService;

    private KafkaConsumer kafkaConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        kafkaConsumer = new KafkaConsumer(emailService, objectMapper);
    }

    // ─────────────────────────────────────────────────
    // TEST 1 — colis-created appelle les 2 bons emails
    // ─────────────────────────────────────────────────
    @Test
    void testEcouterColisCreated_appelleLesBonsEmails() throws Exception {

        // JSON simulé comme si Kafka l'envoyait
        String message = """
            {
                "colisId": "COL-001",
                "expediteurEmail": "jean@gmail.com",
                "destinataireEmail": "marie@gmail.com",
                "description": "Telephone Samsung",
                "statut": "CREE"
            }
            """;

        // On appelle la méthode directement
        kafkaConsumer.ecouterColisCreated(message);

        // On vérifie que EmailService a bien été appelé
        // avec les bons paramètres
        verify(emailService, times(1)).envoyerConfirmationExpediteur(
            "jean@gmail.com", "COL-001", "Telephone Samsung"
        );
        verify(emailService, times(1)).envoyerAvisExpeditionDestinataire(
            "marie@gmail.com", "COL-001", "Telephone Samsung"
        );
    }

    // ─────────────────────────────────────────────────
    // TEST 2 — JSON invalide ne plante pas l'application
    // ─────────────────────────────────────────────────
    @Test
    void testEcouterColisCreated_jsonInvalide_nePlantesPas() {

        // On envoie un JSON complètement invalide
        String messageInvalide = "ceci_nest_pas_du_json";

        // Le consumer doit gérer l'erreur sans planter
        kafkaConsumer.ecouterColisCreated(messageInvalide);

        // EmailService ne doit jamais être appelé
        verifyNoInteractions(emailService);
    }

    // ─────────────────────────────────────────────────
    // TEST 3 — colis-status-changed appelle 2 emails
    // ─────────────────────────────────────────────────
    @Test
    void testEcouterColisStatusChanged_appelleLesBonsEmails() throws Exception {

        String message = """
            {
                "colisId": "COL-001",
                "expediteurEmail": "jean@gmail.com",
                "destinataireEmail": "marie@gmail.com",
                "ancienStatut": "CREE",
                "nouveauStatut": "EN_TRANSIT"
            }
            """;

        kafkaConsumer.ecouterColisStatusChanged(message);

        // Vérifie 2 appels — expéditeur ET destinataire
        verify(emailService, times(1)).envoyerNotificationStatut(
            "jean@gmail.com", "COL-001", "CREE", "EN_TRANSIT"
        );
        verify(emailService, times(1)).envoyerNotificationStatut(
            "marie@gmail.com", "COL-001", "CREE", "EN_TRANSIT"
        );
    }

    // ─────────────────────────────────────────────────
    // TEST 4 — livraison-done appelle 2 emails finaux
    // ─────────────────────────────────────────────────
    @Test
    void testEcouterLivraisonDone_appelleLesBonsEmails() throws Exception {

        String message = """
            {
                "colisId": "COL-001",
                "expediteurEmail": "jean@gmail.com",
                "destinataireEmail": "marie@gmail.com",
                "livreurNom": "Paul Dupont",
                "dateLivraison": "2026-05-25"
            }
            """;

        kafkaConsumer.ecouterLivraisonDone(message);

        verify(emailService, times(1)).envoyerConfirmationLivraisonExpediteur(
            "jean@gmail.com", "COL-001", "Paul Dupont", "2026-05-25"
        );
        verify(emailService, times(1)).envoyerConfirmationLivraisonDestinataire(
            "marie@gmail.com", "COL-001", "Paul Dupont", "2026-05-25"
        );
    }
}