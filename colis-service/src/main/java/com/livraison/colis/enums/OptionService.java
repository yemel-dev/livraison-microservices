package com.livraison.colis.enums;

/**
 * Options de service disponibles pour un colis.
 * Chaque option définit un délai de livraison en jours.
 * La méthode getDelaiJours() est définie dans l'enum (pas dans le service).
 */
public enum OptionService {

    STANDARD(5),
    EXPRESS(2),
    ECONOMIQUE(10);

    private final int delaiJours;

    OptionService(int delaiJours) {
        this.delaiJours = delaiJours;
    }

    /**
     * Retourne le délai de livraison estimé en jours ouvrés.
     */
    public int getDelaiJours() {
        return delaiJours;
    }
}