package com.livraison.livreur.service;

import com.livraison.livreur.dto.CreateLivreurRequest;
import com.livraison.livreur.dto.LivreurResponse;
import com.livraison.livreur.dto.UpdateLivreurRequest;
import com.livraison.livreur.entity.Livreur;
import com.livraison.livreur.exception.ResourceNotFoundException;
import com.livraison.livreur.repository.LivreurRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LivreurService — Tests unitaires")
class LivreurServiceTest {

    @Mock LivreurRepository livreurRepository;
    @InjectMocks LivreurService livreurService;

    private Livreur livreur;

    @BeforeEach
    void setUp() {
        livreur = Livreur.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Moto").actif(true)
                .build();
    }

    // creerLivreur
    @Test
    @DisplayName("creerLivreur — succès : LivreurResponse avec actif=true")
    void creerLivreur_succes() {
        CreateLivreurRequest req = CreateLivreurRequest.builder()
                .nom("Dupont").prenom("Jean").telephone("+237600000001").vehicule("Moto").build();
        when(livreurRepository.existsByTelephone(req.getTelephone())).thenReturn(false);
        when(livreurRepository.save(any(Livreur.class))).thenReturn(livreur);

        LivreurResponse res = livreurService.creerLivreur(req);

        assertThat(res.getNom()).isEqualTo("Dupont");
        assertThat(res.isActif()).isTrue();
        verify(livreurRepository).save(any(Livreur.class));
    }

    @Test
    @DisplayName("creerLivreur — échec si téléphone déjà utilisé")
    void creerLivreur_telephoneDuplique_lanceException() {
        CreateLivreurRequest req = CreateLivreurRequest.builder()
                .nom("X").prenom("Y").telephone("+237600000001").vehicule("Vélo").build();
        when(livreurRepository.existsByTelephone(req.getTelephone())).thenReturn(true);

        assertThatThrownBy(() -> livreurService.creerLivreur(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("+237600000001");
        verify(livreurRepository, never()).save(any());
    }

    // listerLivreursActifs
    @Test
    @DisplayName("listerLivreursActifs — retourne uniquement les actifs")
    void listerLivreursActifs_retourneActifsUniquement() {
        when(livreurRepository.findByActifTrue()).thenReturn(List.of(livreur));
        List<LivreurResponse> result = livreurService.listerLivreursActifs();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActif()).isTrue();
    }

    @Test
    @DisplayName("listerLivreursActifs — liste vide si aucun actif")
    void listerLivreursActifs_listeVide() {
        when(livreurRepository.findByActifTrue()).thenReturn(List.of());
        assertThat(livreurService.listerLivreursActifs()).isEmpty();
    }

    // listerTousLivreurs
    @Test
    @DisplayName("listerTousLivreurs — retourne actifs ET inactifs")
    void listerTousLivreurs_retourneTous() {
        Livreur inactif = Livreur.builder().id(2L).nom("Martin").prenom("Paul")
                .telephone("+237600000002").vehicule("Camion").actif(false).build();
        when(livreurRepository.findAll()).thenReturn(List.of(livreur, inactif));
        assertThat(livreurService.listerTousLivreurs()).hasSize(2);
    }

    // getLivreurById
    @Test
    @DisplayName("getLivreurById — retourne le bon livreur")
    void getLivreurById_retourneLivreur() {
        when(livreurRepository.findById(1L)).thenReturn(Optional.of(livreur));
        LivreurResponse res = livreurService.getLivreurById(1L);
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getNom()).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("getLivreurById — lève ResourceNotFoundException si ID inexistant")
    void getLivreurById_idInexistant_lanceException() {
        when(livreurRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> livreurService.getLivreurById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // mettreAJourLivreur
    @Test
    @DisplayName("mettreAJourLivreur — met à jour uniquement les champs non null")
    void mettreAJourLivreur_metsAJourChampsNonNull() {
        UpdateLivreurRequest req = new UpdateLivreurRequest();
        req.setVehicule("Camion");
        when(livreurRepository.findById(1L)).thenReturn(Optional.of(livreur));
        when(livreurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LivreurResponse res = livreurService.mettreAJourLivreur(1L, req);
        assertThat(res.getVehicule()).isEqualTo("Camion");
        assertThat(res.getNom()).isEqualTo("Dupont"); // non modifié
    }

    @Test
    @DisplayName("mettreAJourLivreur — refuse changement téléphone si déjà pris")
    void mettreAJourLivreur_telephoneDejaUtilise_lanceException() {
        UpdateLivreurRequest req = new UpdateLivreurRequest();
        req.setTelephone("+237600000099");
        when(livreurRepository.findById(1L)).thenReturn(Optional.of(livreur));
        when(livreurRepository.existsByTelephone("+237600000099")).thenReturn(true);

        assertThatThrownBy(() -> livreurService.mettreAJourLivreur(1L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("téléphone");
    }

    // toggleActif
    @Test
    @DisplayName("toggleActif — passe actif=true à false")
    void toggleActif_desactive() {
        when(livreurRepository.findById(1L)).thenReturn(Optional.of(livreur));
        when(livreurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(livreurService.toggleActif(1L).isActif()).isFalse();
    }

    @Test
    @DisplayName("toggleActif — passe actif=false à true")
    void toggleActif_reactive() {
        livreur.setActif(false);
        when(livreurRepository.findById(1L)).thenReturn(Optional.of(livreur));
        when(livreurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(livreurService.toggleActif(1L).isActif()).isTrue();
    }

    // supprimerLivreur
    @Test
    @DisplayName("supprimerLivreur — soft delete : actif=false, pas de DELETE SQL")
    void supprimerLivreur_softDelete() {
        when(livreurRepository.findById(1L)).thenReturn(Optional.of(livreur));
        when(livreurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        livreurService.supprimerLivreur(1L);

        assertThat(livreur.isActif()).isFalse();
        verify(livreurRepository).save(livreur);
        verify(livreurRepository, never()).delete(any());
    }

    @Test
    @DisplayName("supprimerLivreur — lève ResourceNotFoundException si ID inexistant")
    void supprimerLivreur_idInexistant_lanceException() {
        when(livreurRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> livreurService.supprimerLivreur(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
