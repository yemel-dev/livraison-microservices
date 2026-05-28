package com.livraison.colis.service;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.OptionService;
import com.livraison.colis.enums.StatutColis;
import com.livraison.colis.exception.ColisNotFoundException;
import com.livraison.colis.exception.InvalidStatutTransitionException;
import com.livraison.colis.kafka.ColisKafkaProducer;
import com.livraison.colis.mapper.ColisMapper;
import com.livraison.colis.repository.ColisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ColisServiceImpl.
 * Utilise Mockito pour isoler le service de ses dépendances (BDD, Kafka).
 *
 * Scénarios couverts :
 * - Création de colis (numéro de suivi, statut initial)
 * - Transitions de statut valides et invalides
 * - isTermine() sur tous les statuts
 * - Isolation utilisateur (CLIENT ne voit que ses colis)
 * - Suppression (règles métier)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — ColisServiceImpl")
class ColisServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private ColisMapper colisMapper;

    @Mock
    private ColisKafkaProducer kafkaProducer;

    @InjectMocks
    private ColisServiceImpl colisService;

    // ─── Données de test réutilisables ───────────────────────────────────────

    private Colis colisEnAttente;
    private ColisResponseDTO responseDTO;
    private ColisRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        colisEnAttente = Colis.builder()
                .id(1L)
                .numeroSuivi("COL-20240315-A3F7K")
                .expediteurNom("Alice")
                .expediteurAdresse("12 rue de Paris")
                .expediteurEmail("alice@email.com")
                .destinataireNom("Bob")
                .destinataireAdresse("5 avenue de Lyon")
                .destinataireEmail("bob@email.com")
                .poids(2.5)
                .optionService(OptionService.EXPRESS)
                .statut(StatutColis.EN_ATTENTE)
                .createdByUserId(1L)
                .dateCreation(LocalDateTime.now())
                .build();

        responseDTO = ColisResponseDTO.builder()
                .id(1L)
                .numeroSuivi("COL-20240315-A3F7K")
                .statut(StatutColis.EN_ATTENTE)
                .createdByUserId(1L)
                .build();

        requestDTO = ColisRequestDTO.builder()
                .expediteurNom("Alice")
                .expediteurAdresse("12 rue de Paris")
                .expediteurEmail("alice@email.com")
                .destinataireNom("Bob")
                .destinataireAdresse("5 avenue de Lyon")
                .poids(2.5)
                .optionService(OptionService.EXPRESS)
                .build();
    }

    // ─── Création ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("creerColis()")
    class CreerColisTests {

        @Test
        @DisplayName("doit créer un colis avec statut EN_ATTENTE et numéro de suivi")
        void shouldCreateColisWithEnAttenteStatutAndNumeroSuivi() {
            when(colisMapper.toEntity(any(), eq(1L))).thenReturn(colisEnAttente);
            when(colisRepository.existsByNumeroSuivi(anyString())).thenReturn(false);
            when(colisRepository.save(any())).thenReturn(colisEnAttente);
            when(colisMapper.toDTO(any())).thenReturn(responseDTO);

            ColisResponseDTO result = colisService.creerColis(requestDTO, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutColis.EN_ATTENTE);
            assertThat(result.getNumeroSuivi()).startsWith("COL-");
            verify(colisRepository).save(any(Colis.class));
            verify(kafkaProducer).publishColisCreated(any());
        }

        @Test
        @DisplayName("doit générer un numéro de suivi unique en cas de collision")
        void shouldRetryOnNumeroSuiviCollision() {
            when(colisMapper.toEntity(any(), eq(1L))).thenReturn(colisEnAttente);
            // Première tentative : collision. Deuxième tentative : libre.
            when(colisRepository.existsByNumeroSuivi(anyString()))
                    .thenReturn(true)
                    .thenReturn(false);
            when(colisRepository.save(any())).thenReturn(colisEnAttente);
            when(colisMapper.toDTO(any())).thenReturn(responseDTO);

            ColisResponseDTO result = colisService.creerColis(requestDTO, 1L);

            assertThat(result).isNotNull();
            // Vérifie que existsByNumeroSuivi a été appelé 2 fois (1 collision + 1 succès)
            verify(colisRepository, times(2)).existsByNumeroSuivi(anyString());
        }
    }

    // ─── Transitions de statut ───────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatut() — transitions valides")
    class TransitionsValidesTests {

        @Test
        @DisplayName("EN_ATTENTE → ENLEVE doit réussir")
        void enAttenteToEnleveShouldSucceed() {
            testerTransitionValide(StatutColis.EN_ATTENTE, StatutColis.ENLEVE);
        }

        @Test
        @DisplayName("ENLEVE → EN_TRANSIT doit réussir")
        void enleveToEnTransitShouldSucceed() {
            testerTransitionValide(StatutColis.ENLEVE, StatutColis.EN_TRANSIT);
        }

        @Test
        @DisplayName("EN_TRANSIT → EN_LIVRAISON doit réussir")
        void enTransitToEnLivraisonShouldSucceed() {
            testerTransitionValide(StatutColis.EN_TRANSIT, StatutColis.EN_LIVRAISON);
        }

        @Test
        @DisplayName("EN_LIVRAISON → LIVRE doit réussir")
        void enLivraisonToLivreShouldSucceed() {
            testerTransitionValide(StatutColis.EN_LIVRAISON, StatutColis.LIVRE);
        }

        @Test
        @DisplayName("EN_LIVRAISON → ECHEC_LIVRAISON doit réussir")
        void enLivraisonToEchecShouldSucceed() {
            testerTransitionValide(StatutColis.EN_LIVRAISON, StatutColis.ECHEC_LIVRAISON);
        }

        private void testerTransitionValide(StatutColis from, StatutColis to) {
            colisEnAttente.setStatut(from);
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));
            when(colisRepository.save(any())).thenReturn(colisEnAttente);
            when(colisMapper.toDTO(any())).thenReturn(responseDTO);

            assertThatNoException().isThrownBy(() ->
                colisService.updateStatut(1L, to, 1L, "ROLE_ADMIN")
            );
            verify(kafkaProducer).publishStatusChanged(any());
        }
    }

    @Nested
    @DisplayName("updateStatut() — transitions invalides")
    class TransitionsInvalidesTests {

        @Test
        @DisplayName("EN_ATTENTE → LIVRE doit lever InvalidStatutTransitionException")
        void enAttenteToLivreShouldFail() {
            testerTransitionInvalide(StatutColis.EN_ATTENTE, StatutColis.LIVRE);
        }

        @Test
        @DisplayName("EN_ATTENTE → EN_LIVRAISON doit lever InvalidStatutTransitionException")
        void enAttenteToEnLivraisonShouldFail() {
            testerTransitionInvalide(StatutColis.EN_ATTENTE, StatutColis.EN_LIVRAISON);
        }

        @Test
        @DisplayName("LIVRE → EN_ATTENTE doit lever InvalidStatutTransitionException (statut terminal)")
        void livreToEnAttenteShouldFail() {
            testerTransitionInvalide(StatutColis.LIVRE, StatutColis.EN_ATTENTE);
        }

        @Test
        @DisplayName("ECHEC_LIVRAISON → ENLEVE doit lever InvalidStatutTransitionException (statut terminal)")
        void echecToEnleveShouldFail() {
            testerTransitionInvalide(StatutColis.ECHEC_LIVRAISON, StatutColis.ENLEVE);
        }

        private void testerTransitionInvalide(StatutColis from, StatutColis to) {
            colisEnAttente.setStatut(from);
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));

            assertThatThrownBy(() ->
                colisService.updateStatut(1L, to, 1L, "ROLE_ADMIN")
            ).isInstanceOf(InvalidStatutTransitionException.class);

            verify(kafkaProducer, never()).publishStatusChanged(any());
        }
    }

    // ─── isTermine() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTermine()")
    class IsTermineTests {

        @Test
        @DisplayName("LIVRE doit être terminal")
        void livreShouldBeTerminal() {
            colisEnAttente.setStatut(StatutColis.LIVRE);
            assertThat(colisEnAttente.isTermine()).isTrue();
        }

        @Test
        @DisplayName("ECHEC_LIVRAISON doit être terminal")
        void echecShouldBeTerminal() {
            colisEnAttente.setStatut(StatutColis.ECHEC_LIVRAISON);
            assertThat(colisEnAttente.isTermine()).isTrue();
        }

        @Test
        @DisplayName("EN_ATTENTE ne doit pas être terminal")
        void enAttenteShouldNotBeTerminal() {
            assertThat(colisEnAttente.isTermine()).isFalse();
        }

        @Test
        @DisplayName("EN_TRANSIT ne doit pas être terminal")
        void enTransitShouldNotBeTerminal() {
            colisEnAttente.setStatut(StatutColis.EN_TRANSIT);
            assertThat(colisEnAttente.isTermine()).isFalse();
        }
    }

    // ─── Isolation utilisateur ───────────────────────────────────────────────

    @Nested
    @DisplayName("Isolation utilisateur (RBAC)")
    class IsolationTests {

        @Test
        @DisplayName("CLIENT accédant au colis d'un autre utilisateur doit obtenir AccessDeniedException")
        void clientAccessingOtherUserColisShouldThrowAccessDenied() {
            // Le colis appartient à userId=1, mais userId=99 essaie d'y accéder
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));

            assertThatThrownBy(() ->
                colisService.getColisById(1L, 99L, "ROLE_CLIENT")
            ).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("ADMIN peut accéder au colis de n'importe quel utilisateur")
        void adminCanAccessAnyUserColis() {
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));
            when(colisMapper.toDTO(any())).thenReturn(responseDTO);

            assertThatNoException().isThrownBy(() ->
                colisService.getColisById(1L, 99L, "ROLE_ADMIN")
            );
        }

        @Test
        @DisplayName("getAllColis en ROLE_CLIENT ne retourne que les colis de l'utilisateur")
        void getAllColisAsClientReturnsOnlyOwnColis() {
            when(colisRepository.findByCreatedByUserId(1L)).thenReturn(List.of(colisEnAttente));
            when(colisMapper.toDTO(any())).thenReturn(responseDTO);

            List<ColisResponseDTO> result = colisService.getAllColis(1L, "ROLE_CLIENT");

            assertThat(result).hasSize(1);
            verify(colisRepository).findByCreatedByUserId(1L);
            verify(colisRepository, never()).findAll();
        }

        @Test
        @DisplayName("getAllColis en ROLE_ADMIN retourne tous les colis")
        void getAllColisAsAdminReturnsAllColis() {
            when(colisRepository.findAll()).thenReturn(List.of(colisEnAttente));
            when(colisMapper.toDTO(any())).thenReturn(responseDTO);

            colisService.getAllColis(1L, "ROLE_ADMIN");

            verify(colisRepository).findAll();
            verify(colisRepository, never()).findByCreatedByUserId(anyLong());
        }
    }

    // ─── Suppression ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteColis()")
    class DeleteTests {

        @Test
        @DisplayName("supprimer un colis EN_ATTENTE en tant qu'ADMIN doit réussir")
        void adminCanDeleteEnAttenteColis() {
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));

            assertThatNoException().isThrownBy(() ->
                colisService.deleteColis(1L, 1L, "ROLE_ADMIN")
            );
            verify(colisRepository).delete(colisEnAttente);
        }

        @Test
        @DisplayName("supprimer un colis non EN_ATTENTE doit lever IllegalStateException")
        void cannotDeleteNonEnAttenteColis() {
            colisEnAttente.setStatut(StatutColis.EN_TRANSIT);
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));

            assertThatThrownBy(() ->
                colisService.deleteColis(1L, 1L, "ROLE_ADMIN")
            ).isInstanceOf(IllegalStateException.class);

            verify(colisRepository, never()).delete(any());
        }

        @Test
        @DisplayName("un CLIENT ne peut pas supprimer un colis")
        void clientCannotDeleteColis() {
            when(colisRepository.findById(1L)).thenReturn(Optional.of(colisEnAttente));

            assertThatThrownBy(() ->
                colisService.deleteColis(1L, 1L, "ROLE_CLIENT")
            ).isInstanceOf(AccessDeniedException.class);

            verify(colisRepository, never()).delete(any());
        }

        @Test
        @DisplayName("supprimer un colis inexistant doit lever ColisNotFoundException")
        void deleteNonExistentColisShouldThrow() {
            when(colisRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                colisService.deleteColis(99L, 1L, "ROLE_ADMIN")
            ).isInstanceOf(ColisNotFoundException.class);
        }
    }
}