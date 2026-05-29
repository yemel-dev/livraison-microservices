package com.livraison.notification.event;

public class LivraisonDoneEvent {

    private String colisId;
    private String expediteurEmail;
    private String destinataireEmail;
    private String livreurNom;
    private String dateLivraison;

    public LivraisonDoneEvent() {}

    public LivraisonDoneEvent(String colisId, String expediteurEmail,
                               String destinataireEmail, String livreurNom, String dateLivraison) {
        this.colisId = colisId;
        this.expediteurEmail = expediteurEmail;
        this.destinataireEmail = destinataireEmail;
        this.livreurNom = livreurNom;
        this.dateLivraison = dateLivraison;
    }

    public String getColisId() { return colisId; }
    public String getExpediteurEmail() { return expediteurEmail; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public String getLivreurNom() { return livreurNom; }
    public String getDateLivraison() { return dateLivraison; }

    public void setColisId(String colisId) { this.colisId = colisId; }
    public void setExpediteurEmail(String expediteurEmail) { this.expediteurEmail = expediteurEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public void setLivreurNom(String livreurNom) { this.livreurNom = livreurNom; }
    public void setDateLivraison(String dateLivraison) { this.dateLivraison = dateLivraison; }
}