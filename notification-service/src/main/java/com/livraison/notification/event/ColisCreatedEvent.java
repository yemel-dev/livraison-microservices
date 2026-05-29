package com.livraison.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Miroir de l'événement publié par le colis-service sur le topic "colis.created".
 * Les champs correspondent exactement à ce que le colis-service envoie.
 * @JsonIgnoreProperties ignore les champs inconnus pour éviter les erreurs de désérialisation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColisCreatedEvent {

    private Long colisId;
    private String numeroSuivi;
    private String expediteurNom;
    private String expediteurEmail;
    private String destinataireNom;
    private String destinataireEmail;
    private String optionService;
    private int delaiLivraisonJours;
    private LocalDateTime dateCreation;
    private Long createdByUserId;

    public ColisCreatedEvent() {}

    public Long getColisId() { return colisId; }
    public String getNumeroSuivi() { return numeroSuivi; }
    public String getExpediteurNom() { return expediteurNom; }
    public String getExpediteurEmail() { return expediteurEmail; }
    public String getDestinataireNom() { return destinataireNom; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public String getOptionService() { return optionService; }
    public int getDelaiLivraisonJours() { return delaiLivraisonJours; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public Long getCreatedByUserId() { return createdByUserId; }

    public void setColisId(Long colisId) { this.colisId = colisId; }
    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }
    public void setExpediteurNom(String expediteurNom) { this.expediteurNom = expediteurNom; }
    public void setExpediteurEmail(String expediteurEmail) { this.expediteurEmail = expediteurEmail; }
    public void setDestinataireNom(String destinataireNom) { this.destinataireNom = destinataireNom; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public void setOptionService(String optionService) { this.optionService = optionService; }
    public void setDelaiLivraisonJours(int delaiLivraisonJours) { this.delaiLivraisonJours = delaiLivraisonJours; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
}