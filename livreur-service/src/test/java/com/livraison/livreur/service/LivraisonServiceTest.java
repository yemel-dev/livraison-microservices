package com.livraison.livreur.service;

import com.livraison.livreur.dto.AssignerLivraisonRequest;
import com.livraison.livreur.dto.EchecRequest;
import com.livraison.livreur.dto.LivraisonResponse;
import com.livraison.livreur.entity.Livraison;
import com.livraison.livreur.entity.Livreur;
import com.livraison.livreur.enums.StatutLivraison;
import com.livraison.livreur.exception.AccessDeniedException;
import com.livraison.livreur.exception.ResourceNotFoundException;
import com.livraison.livreur.kafka.KafkaProducerService;
import com.livraison.livreur.repository.LivraisonRepository;
import com.livraison.livreur.security.SecurityContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LivraisonService — Tests unitaires")
class LivraisonServiceTest {

    @Mock LivraisonRepository livraisonRepository;
    @Mock LivreurService livreurService;
    @Mock KafkaProducerService kafkaProducerService;
    @InjectMocks LivraisonService livraisonService;

    private Livreur livreur;
    private Livraison livraison;

    @BeforeEach
    void setUp() {
        livreur = Livreur.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Moto").actif(true)
                .build();

        livraison = Livraison.builder()
                .id(10L)
                .numeroSuivi("TRK-2024-001")
                .livreur(livreur)
                .statut(StatutLivraison.ASSIGNEE)
                .dateAssignation(LocalDateTime.now())
                .build();
    }

    // ─── assignerColis ────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignerColis — succès : livraison créée avec statut ASSIGNEE")
    void assignerColis_succes() {
        AssignerLivraisonRequest req = new AssignerLivraisonRequest();
        req.setNumeroSuivi("TRK-001"); req.setLivreurId(1L);

        when(livreurService.findLivreurOuErreur(1L)).thenReturn(livreur);
        when(livraisonRepository.save(any(Livraison.class))).thenReturn(livraison);

        LivraisonResponse res = livraisonService.assignerColis(req);

        assertThat(res.getStatut()).isEqualTo(StatutLivraison.ASSIGNEE);
        assertThat(res.getNumeroSuivi()).isEqualTo("TRK-2024-001");
        verify(kafkaProducerService).publierColisStatusChanged(any());
    }

    @Test
    @DisplayName("assignerColis — refuse si le livreur est inactif")
    void assignerColis_livreurInactif_lanceException() {
        livreur.setActif(false);
        AssignerLivraisonRequest req = new AssignerLivraisonRequest();
        req.setNumeroSuivi("TRK-001"); req.setLivreurId(1L);

        when(livreurService.findLivreurOuErreur(1L)).thenReturn(livreur);

        assertThatThrownBy(() -> livraisonService.assignerColis(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("désactivé");
        verify(livraisonRepository, never()).save(any());
    }

    // ─── scannerPriseEnCharge ─────────────────────────────────────────────────

    @Test
    @DisplayName("scannerPriseEnCharge — ASSIGNEE → ENLEVEE")
    void scannerPriseEnCharge_assigneeVersEnlevee() {
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));
        when(livraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LivraisonResponse res = livraisonService.scannerPriseEnCharge(10L, 1L);

        assertThat(res.getStatut()).isEqualTo(StatutLivraison.ENLEVEE);
        verify(kafkaProducerService).publierColisStatusChanged(any());
    }

    @Test
    @DisplayName("scannerPriseEnCharge — refuse si statut != ASSIGNEE")
    void scannerPriseEnCharge_statutInvalide_lanceException() {
        livraison.setStatut(StatutLivraison.EN_COURS);
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));

        assertThatThrownBy(() -> livraisonService.scannerPriseEnCharge(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ASSIGNEE");
    }

    @Test
    @DisplayName("scannerPriseEnCharge — AccessDeniedException si mauvais livreur")
    void scannerPriseEnCharge_mauvaisLivreur_lanceAccessDenied() {
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));

        assertThatThrownBy(() -> livraisonService.scannerPriseEnCharge(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── mettreEnTransit ──────────────────────────────────────────────────────

    @Test
    @DisplayName("mettreEnTransit — ENLEVEE → EN_COURS")
    void mettreEnTransit_enleveeVersEnCours() {
        livraison.setStatut(StatutLivraison.ENLEVEE);
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));
        when(livraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LivraisonResponse res = livraisonService.mettreEnTransit(10L, 1L);

        assertThat(res.getStatut()).isEqualTo(StatutLivraison.EN_COURS);
        verify(kafkaProducerService).publierColisStatusChanged(any());
    }

    @Test
    @DisplayName("mettreEnTransit — refuse si statut != ENLEVEE")
    void mettreEnTransit_statutInvalide_lanceException() {
        livraison.setStatut(StatutLivraison.ASSIGNEE);
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));

        assertThatThrownBy(() -> livraisonService.mettreEnTransit(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENLEVEE");
    }

    // ─── confirmerLivraison ───────────────────────────────────────────────────

    @Test
    @DisplayName("confirmerLivraison — EN_COURS → LIVREE avec dateLivraison renseignée")
    void confirmerLivraison_enCoursVersLivree() {
        livraison.setStatut(StatutLivraison.EN_COURS);
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));
        when(livraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LivraisonResponse res = livraisonService.confirmerLivraison(10L, 1L);

        assertThat(res.getStatut()).isEqualTo(StatutLivraison.LIVREE);
        assertThat(res.getDateLivraison()).isNotNull();
        verify(kafkaProducerService).publierLivraisonDone(any());
        verify(kafkaProducerService).publierColisStatusChanged(any());
    }

    @Test
    @DisplayName("confirmerLivraison — refuse si livraison déjà terminée (LIVREE)")
    void confirmerLivraison_dejàTerminee_lanceException() {
        livraison.setStatut(StatutLivraison.LIVREE);
        livraison.setDateLivraison(LocalDateTime.now());
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));

        assertThatThrownBy(() -> livraisonService.confirmerLivraison(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminée");
    }

    // ─── enregistrerEchec ─────────────────────────────────────────────────────

    @Test
    @DisplayName("enregistrerEchec — passe en statut ECHEC avec motif renseigné")
    void enregistrerEchec_passEnEchec() {
        livraison.setStatut(StatutLivraison.EN_COURS);
        EchecRequest req = new EchecRequest();
        req.setMotifEchec("Destinataire absent");

        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));
        when(livraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LivraisonResponse res = livraisonService.enregistrerEchec(10L, 1L, req);

        assertThat(res.getStatut()).isEqualTo(StatutLivraison.ECHEC);
        assertThat(res.getMotifEchec()).isEqualTo("Destinataire absent");
        verify(kafkaProducerService).publierColisStatusChanged(any());
    }

    @Test
    @DisplayName("enregistrerEchec — refuse si livraison déjà terminée")
    void enregistrerEchec_dejaTerminee_lanceException() {
        livraison.setStatut(StatutLivraison.LIVREE);
        livraison.setDateLivraison(LocalDateTime.now());
        EchecRequest req = new EchecRequest();
        req.setMotifEchec("Motif");

        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));

        assertThatThrownBy(() -> livraisonService.enregistrerEchec(10L, 1L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminée");
    }

    @Test
    @DisplayName("enregistrerEchec — refuse si mauvais livreurId (Zero Trust)")
    void enregistrerEchec_mauvaisLivreur_lanceAccessDenied() {
        livraison.setStatut(StatutLivraison.EN_COURS);
        EchecRequest req = new EchecRequest();
        req.setMotifEchec("Motif");

        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));

        assertThatThrownBy(() -> livraisonService.enregistrerEchec(10L, 999L, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── getLivraison ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLivraison — retourne la livraison si trouvée")
    void getLivraison_retourneLivraison() {
        when(livraisonRepository.findById(10L)).thenReturn(Optional.of(livraison));
        LivraisonResponse res = livraisonService.getLivraison(10L);
        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getNumeroSuivi()).isEqualTo("TRK-2024-001");
    }

    @Test
    @DisplayName("getLivraison — lève ResourceNotFoundException si ID inexistant")
    void getLivraison_idInexistant_lanceException() {
        when(livraisonRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> livraisonService.getLivraison(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
