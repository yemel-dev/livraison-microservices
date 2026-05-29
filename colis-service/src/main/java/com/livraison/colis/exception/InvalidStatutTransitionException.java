package com.livraison.colis.exception;

import com.livraison.colis.enums.StatutColis;

/**
 * Lancée quand une transition de statut est invalide.
 * Ex : tenter de passer directement de EN_ATTENTE à LIVRE.
 * → HTTP 400 Bad Request (géré par GlobalExceptionHandler)
 */
public class InvalidStatutTransitionException extends RuntimeException {

    private final StatutColis statutActuel;
    private final StatutColis statutCible;

    public InvalidStatutTransitionException(StatutColis statutActuel, StatutColis statutCible) {
        super(String.format(
            "Transition de statut invalide : %s → %s. Transitions autorisées : %s",
            statutActuel, statutCible, getTransitionsAutorisees(statutActuel)
        ));
        this.statutActuel = statutActuel;
        this.statutCible  = statutCible;
    }

    public StatutColis getStatutActuel() {
        return statutActuel;
    }

    public StatutColis getStatutCible() {
        return statutCible;
    }

    /**
     * Retourne un message lisible des transitions autorisées depuis un statut donné.
     */
    private static String getTransitionsAutorisees(StatutColis statut) {
        return switch (statut) {
            case EN_ATTENTE   -> "EN_ATTENTE → ENLEVE";
            case ENLEVE       -> "ENLEVE → EN_TRANSIT";
            case EN_TRANSIT   -> "EN_TRANSIT → EN_LIVRAISON";
            case EN_LIVRAISON -> "EN_LIVRAISON → LIVRE ou ECHEC_LIVRAISON";
            case LIVRE, ECHEC_LIVRAISON -> "aucune (statut terminal)";
        };
    }
}