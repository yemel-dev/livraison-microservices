package com.livraison.notification.event;

public class ColisStatusChangedEvent {

    private String colisId;
    private String expediteurEmail;
    private String destinataireEmail;
    private String ancienStatut;
    private String nouveauStatut;

    public ColisStatusChangedEvent() {}

    public ColisStatusChangedEvent(String colisId, String expediteurEmail,
                                    String destinataireEmail, String ancienStatut, String nouveauStatut) {
        this.colisId = colisId;
        this.expediteurEmail = expediteurEmail;
        this.destinataireEmail = destinataireEmail;
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
    }

    public String getColisId() { return colisId; }
    public String getExpediteurEmail() { return expediteurEmail; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public String getAncienStatut() { return ancienStatut; }
    public String getNouveauStatut() { return nouveauStatut; }

    public void setColisId(String colisId) { this.colisId = colisId; }
    public void setExpediteurEmail(String expediteurEmail) { this.expediteurEmail = expediteurEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public void setAncienStatut(String ancienStatut) { this.ancienStatut = ancienStatut; }
    public void setNouveauStatut(String nouveauStatut) { this.nouveauStatut = nouveauStatut; }
}