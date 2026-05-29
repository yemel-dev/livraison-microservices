package com.livraison.notification.service;

// Permet à Spring de détecter cette classe automatiquement
// et de la gérer comme un composant de l'application
import org.springframework.stereotype.Service;

// Logger : permet d'afficher des messages dans la console
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simule l'envoi d'emails par des logs console.
 * En production, on remplacerait les logs
 * par un vrai client SMTP (ex: JavaMailSender)
 * Sans changer le reste du code. ✅
 */
@Service
public class EmailService {

    // On crée un logger attaché à cette classe
    // Tous les logs afficheront "EmailService" comme source
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // ─────────────────────────────────────────────────
    // MÉTHODE 1 — Email de confirmation à l'expéditeur
    // Appelée quand un colis est créé
    // ─────────────────────────────────────────────────
    public void envoyerConfirmationExpediteur(String email, String colisId, String description) {

        // On simule l'envoi en loggant le contenu de l'email
        log.info("======================================");
        log.info("📧 EMAIL ENVOYÉ À L'EXPÉDITEUR");
        log.info("À        : {}", email);
        log.info("Sujet    : Votre colis {} a bien été créé", colisId);
        log.info("Message  : Bonjour, votre colis contenant '{}' " +
                 "a été enregistré avec l'identifiant {}. " +
                 "Vous serez notifié à chaque étape.", description, colisId);
        log.info("======================================");
    }

    // ─────────────────────────────────────────────────
    // MÉTHODE 2 — Email d'avis d'expédition au destinataire
    // Appelée quand un colis est créé
    // ─────────────────────────────────────────────────
    public void envoyerAvisExpeditionDestinataire(String email, String colisId, String description) {

        log.info("======================================");
        log.info("📧 EMAIL ENVOYÉ AU DESTINATAIRE");
        log.info("À        : {}", email);
        log.info("Sujet    : Un colis est en route pour vous !");
        log.info("Message  : Bonjour, un colis contenant '{}' " +
                 "a été expédié pour vous. " +
                 "Référence : {}.", description, colisId);
        log.info("======================================");
    }

    // ─────────────────────────────────────────────────
    // MÉTHODE 3 — Email de changement de statut
    // Appelée quand le statut d'un colis change
    // Envoyée à l'expéditeur ET au destinataire
    // ─────────────────────────────────────────────────
    public void envoyerNotificationStatut(String email, String colisId,
                                          String ancienStatut, String nouveauStatut) {

        log.info("======================================");
        log.info("📧 EMAIL CHANGEMENT DE STATUT");
        log.info("À        : {}", email);
        log.info("Sujet    : Mise à jour de votre colis {}", colisId);
        log.info("Message  : Le statut de votre colis {} " +
                 "est passé de '{}' à '{}'.", colisId, ancienStatut, nouveauStatut);
        log.info("======================================");
    }

    // ─────────────────────────────────────────────────
    // MÉTHODE 4 — Email de confirmation finale expéditeur
    // Appelée quand la livraison est terminée
    // ─────────────────────────────────────────────────
    public void envoyerConfirmationLivraisonExpediteur(String email, String colisId,
                                                        String livreurNom, String dateLivraison) {

        log.info("======================================");
        log.info("📧 EMAIL LIVRAISON CONFIRMÉE - EXPÉDITEUR");
        log.info("À        : {}", email);
        log.info("Sujet    : Votre colis {} a été livré ✅", colisId);
        log.info("Message  : Votre colis {} a été livré le {} " +
                 "par {}.", colisId, dateLivraison, livreurNom);
        log.info("======================================");
    }

    // ─────────────────────────────────────────────────
    // MÉTHODE 5 — Email de confirmation finale destinataire
    // Appelée quand la livraison est terminée
    // ─────────────────────────────────────────────────
    public void envoyerConfirmationLivraisonDestinataire(String email, String colisId,
                                                          String livreurNom, String dateLivraison) {

        log.info("======================================");
        log.info("📧 EMAIL LIVRAISON REÇUE - DESTINATAIRE");
        log.info("À        : {}", email);
        log.info("Sujet    : Vous avez reçu votre colis {} 🎉", colisId);
        log.info("Message  : Votre colis {} vous a été remis le {} " +
                 "par {}. Merci de votre confiance !", colisId, dateLivraison, livreurNom);
        log.info("======================================");
    }
}