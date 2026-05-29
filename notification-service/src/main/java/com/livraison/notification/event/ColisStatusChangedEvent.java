package com.livraison.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Miroir de l'événement publié par le colis-service sur le topic "colis.status_changed".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColisStatusChangedEvent {

    private Long colisId;
    private String numeroSuivi;
    private String ancienStatut;
    private String nouveauStatut;
    private String destinataireEmail;
    private String destinataireNom;
    private LocalDateTime dateMiseAJour;
    private Long modifiePar;

    public ColisStatusChangedEvent() {}

    public Long getColisId() { return colisId; }
    public String getNumeroSuivi() { return numeroSuivi; }
    public String getAncienStatut() { return ancienStatut; }
    public String getNouveauStatut() { return nouveauStatut; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public String getDestinataireNom() { return destinataireNom; }
    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public Long getModifiePar() { return modifiePar; }

    public void setColisId(Long colisId) { this.colisId = colisId; }
    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }
    public void setAncienStatut(String ancienStatut) { this.ancienStatut = ancienStatut; }
    public void setNouveauStatut(String nouveauStatut) { this.nouveauStatut = nouveauStatut; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public void setDestinataireNom(String destinataireNom) { this.destinataireNom = destinataireNom; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }
    public void setModifiePar(Long modifiePar) { this.modifiePar = modifiePar; }
}