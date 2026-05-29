package com.livraison.colis.enums;

/**
 * Cycle de vie d'un colis.
 * Transitions autorisées (strictement) :
 *   EN_ATTENTE → ENLEVE → EN_TRANSIT → EN_LIVRAISON → LIVRE
 *                                                    ↘ ECHEC_LIVRAISON
 *
 * LIVRE et ECHEC_LIVRAISON sont des statuts terminaux : aucune transition possible.
 */
public enum StatutColis {

    EN_ATTENTE,
    ENLEVE,
    EN_TRANSIT,
    EN_LIVRAISON,
    LIVRE,
    ECHEC_LIVRAISON;

    /**
     * Vérifie si une transition vers le statut cible est autorisée depuis ce statut.
     *
     * @param cible le statut vers lequel on veut transitionner
     * @return true si la transition est valide
     */
    public boolean peutTransitionnerVers(StatutColis cible) {
        return switch (this) {
            case EN_ATTENTE      -> cible == ENLEVE;
            case ENLEVE          -> cible == EN_TRANSIT;
            case EN_TRANSIT      -> cible == EN_LIVRAISON;
            case EN_LIVRAISON    -> cible == LIVRE || cible == ECHEC_LIVRAISON;
            // Statuts terminaux : aucune transition possible
            case LIVRE, ECHEC_LIVRAISON -> false;
        };
    }

    /**
     * Indique si ce statut est terminal (plus aucun changement possible).
     */
    public boolean estTerminal() {
        return this == LIVRE || this == ECHEC_LIVRAISON;
    }
}