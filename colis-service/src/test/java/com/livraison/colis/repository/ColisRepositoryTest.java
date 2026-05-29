package com.livraison.colis.repository;

import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.OptionService;
import com.livraison.colis.enums.StatutColis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration du repository JPA.
 *
 * @DataJpaTest :
 * - Charge uniquement la couche JPA (pas tout Spring)
 * - Utilise H2 en mémoire (défini dans application-test.yml)
 * - Chaque test s'exécute dans une transaction rollbackée → BDD propre à chaque test
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration — ColisRepository")
class ColisRepositoryTest {

    @Autowired
    private ColisRepository colisRepository;

    private Colis colis1;
    private Colis colis2;

    @BeforeEach
    void setUp() {
        colisRepository.deleteAll();

        colis1 = colisRepository.save(Colis.builder()
                .numeroSuivi("COL-20240315-AAAAA")
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
                .build());

        colis2 = colisRepository.save(Colis.builder()
                .numeroSuivi("COL-20240315-BBBBB")
                .expediteurNom("Charlie")
                .expediteurAdresse("3 rue de Lyon")
                .expediteurEmail("charlie@email.com")
                .destinataireNom("Diana")
                .destinataireAdresse("8 rue de Marseille")
                .poids(1.0)
                .optionService(OptionService.STANDARD)
                .statut(StatutColis.EN_TRANSIT)
                .createdByUserId(2L)
                .build());
    }

    @Test
    @DisplayName("findByNumeroSuivi doit retourner le bon colis")
    void findByNumeroSuiviShouldReturnColis() {
        Optional<Colis> result = colisRepository.findByNumeroSuivi("COL-20240315-AAAAA");

        assertThat(result).isPresent();
        assertThat(result.get().getExpediteurNom()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("findByNumeroSuivi doit retourner vide pour un numéro inexistant")
    void findByNumeroSuiviShouldReturnEmptyForUnknown() {
        Optional<Colis> result = colisRepository.findByNumeroSuivi("COL-INCONNU-XXXXX");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByCreatedByUserId doit retourner uniquement les colis de l'utilisateur")
    void findByCreatedByUserIdShouldReturnOnlyUserColis() {
        List<Colis> result = colisRepository.findByCreatedByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumeroSuivi()).isEqualTo("COL-20240315-AAAAA");
    }

    @Test
    @DisplayName("findByStatut doit retourner les colis avec le statut donné")
    void findByStatutShouldReturnMatchingColis() {
        List<Colis> enAttente = colisRepository.findByStatut(StatutColis.EN_ATTENTE);
        List<Colis> enTransit = colisRepository.findByStatut(StatutColis.EN_TRANSIT);

        assertThat(enAttente).hasSize(1);
        assertThat(enTransit).hasSize(1);
    }

    @Test
    @DisplayName("existsByNumeroSuivi doit retourner true si le numéro existe")
    void existsByNumeroSuiviShouldReturnTrueIfExists() {
        assertThat(colisRepository.existsByNumeroSuivi("COL-20240315-AAAAA")).isTrue();
        assertThat(colisRepository.existsByNumeroSuivi("COL-INEXISTANT-XXXXX")).isFalse();
    }

    @Test
    @DisplayName("findByCreatedByUserIdAndStatut doit combiner les deux filtres")
    void findByCreatedByUserIdAndStatutShouldCombineFilters() {
        List<Colis> result = colisRepository.findByCreatedByUserIdAndStatut(1L, StatutColis.EN_ATTENTE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCreatedByUserId()).isEqualTo(1L);
        assertThat(result.get(0).getStatut()).isEqualTo(StatutColis.EN_ATTENTE);
    }

    @Test
    @DisplayName("la contrainte UNIQUE sur numeroSuivi doit empêcher les doublons")
    void uniqueConstraintOnNumeroSuiviShouldPreventDuplicates() {
        Colis doublon = Colis.builder()
                .numeroSuivi("COL-20240315-AAAAA") // même numéro que colis1
                .expediteurNom("Test")
                .expediteurAdresse("Test adresse")
                .expediteurEmail("test@email.com")
                .destinataireNom("Test dest")
                .destinataireAdresse("Test dest adresse")
                .poids(1.0)
                .optionService(OptionService.STANDARD)
                .statut(StatutColis.EN_ATTENTE)
                .createdByUserId(3L)
                .build();

        // Doit lever une exception de contrainte
        org.junit.jupiter.api.Assertions.assertThrows(
            Exception.class,
            () -> colisRepository.saveAndFlush(doublon)
        );
    }
}