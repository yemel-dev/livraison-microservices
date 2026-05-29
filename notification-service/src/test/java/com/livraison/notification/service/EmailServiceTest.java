package com.livraison.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test unitaire de EmailService. On vérifie que chaque méthode s'exécute sans
 * erreur et sans avoir besoin de Kafka.
 */
public class EmailServiceTest {

    // On crée directement le service — pas besoin de Spring
    private EmailService emailService;

    // S'exécute avant chaque test
    @BeforeEach
    void setUp() {
        emailService = new EmailService();
    }

    @Test
    void testEnvoyerConfirmationExpediteur() {
        // Vérifie que la méthode ne lance pas d'exception
        emailService.envoyerConfirmationExpediteur(
                "jean@gmail.com",
                "COL-001",
                "Telephone Samsung"
        );
        // Si on arrive ici sans exception → test réussi ✅
    }

    @Test
    void testEnvoyerAvisExpeditionDestinataire() {
        emailService.envoyerAvisExpeditionDestinataire(
                "marie@gmail.com",
                "COL-001",
                "Telephone Samsung"
        );
    }

    @Test
    void testEnvoyerNotificationStatut() {
        emailService.envoyerNotificationStatut(
                "jean@gmail.com",
                "COL-001",
                "CREE",
                "EN_TRANSIT"
        );
    }

    @Test
    void testEnvoyerConfirmationLivraisonExpediteur() {
        emailService.envoyerConfirmationLivraisonExpediteur(
                "jean@gmail.com",
                "COL-001",
                "Paul Dupont",
                "2026-05-25"
        );
    }

    @Test
    void testEnvoyerConfirmationLivraisonDestinataire() {
        emailService.envoyerConfirmationLivraisonDestinataire(
                "marie@gmail.com",
                "COL-001",
                "Paul Dupont",
                "2026-05-25"
        );
    }
}
