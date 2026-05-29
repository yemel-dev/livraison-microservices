package com.livraison.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Miroir de l'événement publié par le livreur-service sur le topic "livraison.done".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivraisonDoneEvent {

    private String numeroSuivi;
    private Long livreurId;
    private String livreurNom;
    private LocalDateTime dateLivraison;
    private String eventType;

    public LivraisonDoneEvent() {}

    public String getNumeroSuivi() { return numeroSuivi; }
    public Long getLivreurId() { return livreurId; }
    public String getLivreurNom() { return livreurNom; }
    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public String getEventType() { return eventType; }

    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }
    public void setLivreurId(Long livreurId) { this.livreurId = livreurId; }
    public void setLivreurNom(String livreurNom) { this.livreurNom = livreurNom; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}