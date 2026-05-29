package com.livraison.livreur.entity;

import com.livraison.livreur.enums.StatutLivraison;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Livraison Entity — Tests unitaires méthodes métier")
class LivraisonEntityTest {

    private Livraison livraison;

    @BeforeEach
    void setUp() {
        Livreur livreur = Livreur.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Moto").actif(true).build();

        livraison = Livraison.builder()
                .id(1L).numeroSuivi("TRK-001")
                .livreur(livreur)
                .statut(StatutLivraison.EN_COURS)
                .dateAssignation(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("confirmerLivraison — passe en LIVREE et renseigne dateLivraison")
    void confirmerLivraison_passeLivreeAvecDate() {
        livraison.confirmerLivraison();

        assertThat(livraison.getStatut()).isEqualTo(StatutLivraison.LIVREE);
        assertThat(livraison.getDateLivraison()).isNotNull();
        assertThat(livraison.getDateLivraison()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("enregistrerEchec — passe en ECHEC avec motif et date")
    void enregistrerEchec_passeEchecAvecMotif() {
        livraison.enregistrerEchec("Destinataire absent");

        assertThat(livraison.getStatut()).isEqualTo(StatutLivraison.ECHEC);
        assertThat(livraison.getMotifEchec()).isEqualTo("Destinataire absent");
        assertThat(livraison.getDateLivraison()).isNotNull();
    }

    @Test
    @DisplayName("isTermine — retourne true si statut LIVREE")
    void isTermine_livree_retourneTrue() {
        livraison.confirmerLivraison();
        assertThat(livraison.isTermine()).isTrue();
    }

    @Test
    @DisplayName("isTermine — retourne true si statut ECHEC")
    void isTermine_echec_retourneTrue() {
        livraison.enregistrerEchec("Adresse introuvable");
        assertThat(livraison.isTermine()).isTrue();
    }

    @Test
    @DisplayName("isTermine — retourne false si statut EN_COURS")
    void isTermine_enCours_retourneFalse() {
        assertThat(livraison.isTermine()).isFalse();
    }

    @Test
    @DisplayName("isTermine — retourne false si statut ASSIGNEE")
    void isTermine_assignee_retourneFalse() {
        livraison.setStatut(StatutLivraison.ASSIGNEE);
        assertThat(livraison.isTermine()).isFalse();
    }

    @Test
    @DisplayName("isTermine — retourne false si statut ENLEVEE")
    void isTermine_enlevee_retourneFalse() {
        livraison.setStatut(StatutLivraison.ENLEVEE);
        assertThat(livraison.isTermine()).isFalse();
    }

    @Test
    @DisplayName("confirmerLivraison deux fois — la deuxième date est plus récente")
    void confirmerLivraison_deuxFois_dateMiseAJour() throws InterruptedException {
        livraison.confirmerLivraison();
        LocalDateTime premiere = livraison.getDateLivraison();
        Thread.sleep(10);
        livraison.confirmerLivraison();
        assertThat(livraison.getDateLivraison()).isAfterOrEqualTo(premiere);
    }
}
