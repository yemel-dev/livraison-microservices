package com.livraison.colis.exception;

/**
 * Lancée quand un colis n'existe pas en base de données.
 * → HTTP 404 Not Found (géré par GlobalExceptionHandler)
 */
public class ColisNotFoundException extends RuntimeException {

    private final Long id;
    private final String numeroSuivi;

    /**
     * Recherche par ID
     */
    public ColisNotFoundException(Long id) {
        super("Colis introuvable avec l'id : " + id);
        this.id = id;
        this.numeroSuivi = null;
    }

    /**
     * Recherche par numéro de suivi
     */
    public ColisNotFoundException(String numeroSuivi) {
        super("Colis introuvable avec le numéro de suivi : " + numeroSuivi);
        this.id = null;
        this.numeroSuivi = numeroSuivi;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroSuivi() {
        return numeroSuivi;
    }
}