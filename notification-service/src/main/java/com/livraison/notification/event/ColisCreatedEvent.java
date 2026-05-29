package com.livraison.notification.event;

public class ColisCreatedEvent {

    private String colisId;
    private String expediteurEmail;
    private String destinataireEmail;
    private String description;
    private String statut;

    public ColisCreatedEvent() {}

    public ColisCreatedEvent(String colisId, String expediteurEmail,
                              String destinataireEmail, String description, String statut) {
        this.colisId = colisId;
        this.expediteurEmail = expediteurEmail;
        this.destinataireEmail = destinataireEmail;
        this.description = description;
        this.statut = statut;
    }

    public String getColisId() { return colisId; }
    public String getExpediteurEmail() { return expediteurEmail; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public String getDescription() { return description; }
    public String getStatut() { return statut; }

    public void setColisId(String colisId) { this.colisId = colisId; }
    public void setExpediteurEmail(String expediteurEmail) { this.expediteurEmail = expediteurEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public void setDescription(String description) { this.description = description; }
    public void setStatut(String statut) { this.statut = statut; }
}