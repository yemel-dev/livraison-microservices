package com.livraison.user.model;

/**
 * Rôles possibles d'un utilisateur dans la plateforme.
 * Stocké en base comme String grâce à @Enumerated(EnumType.STRING).
 */
public enum Role {
    CLIENT,   // Peut créer et suivre ses colis
    LIVREUR,  // Gère sa tournée et confirme les livraisons
    ADMIN     // Accès complet à tous les colis et livreurs
}