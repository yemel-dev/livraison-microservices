package com.livraison.colis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.colis.config.OpenApiConfig;
import com.livraison.colis.config.SecurityConfig;
import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.dto.UpdateStatutDTO;
import com.livraison.colis.enums.OptionService;
import com.livraison.colis.enums.StatutColis;
import com.livraison.colis.exception.ColisNotFoundException;
import com.livraison.colis.exception.InvalidStatutTransitionException;
import com.livraison.colis.security.HeaderAuthenticationFilter;
import com.livraison.colis.service.ColisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests REST du ColisController avec MockMvc.
 *
 * On importe SecurityConfig + HeaderAuthenticationFilter pour tester
 * le vrai comportement de sécurité (401 sans headers, 403 sans droits).
 *
 * OpenApiConfig est exclu car SpringDoc scanne les beans au runtime
 * et peut provoquer des erreurs dans le contexte de test allégé de @WebMvcTest.
 */
@WebMvcTest(
    controllers = ColisController.class,
    excludeAutoConfiguration = {
        // Exclure l'autoconfig SpringDoc pour éviter les conflits dans le contexte de test
        org.springdoc.webmvc.ui.SwaggerConfig.class
    }
)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
@ActiveProfiles("test")
@DisplayName("Tests REST — ColisController")
class ColisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ColisService colisService;

    // OpenApiConfig est mocké pour éviter que SpringDoc essaie de
    // scanner les controllers dans un contexte incomplet
    @MockBean
    private OpenApiConfig openApiConfig;

    private ColisResponseDTO responseDTO;
    private ColisRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = ColisResponseDTO.builder()
                .id(1L)
                .numeroSuivi("COL-20240315-A3F7K")
                .expediteurNom("Alice")
                .destinataireNom("Bob")
                .poids(2.5)
                .optionService(OptionService.EXPRESS)
                .delaiLivraisonJours(2)
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

    // ─── POST /api/colis ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/colis")
    class PostColisTests {

        @Test
        @DisplayName("avec headers valides et DTO valide → 201 Created")
        void withValidHeadersAndDto_shouldReturn201() throws Exception {
            when(colisService.creerColis(any(), eq(1L))).thenReturn(responseDTO);

            mockMvc.perform(post("/api/colis")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_CLIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numeroSuivi").value("COL-20240315-A3F7K"))
                    .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
        }

        @Test
        @DisplayName("sans header X-User-Id → 401 Unauthorized")
        void withoutUserIdHeader_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/colis")
                            .header("X-User-Role", "ROLE_CLIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("avec DTO invalide (poids manquant) → 400 Bad Request")
        void withInvalidDto_shouldReturn400() throws Exception {
            ColisRequestDTO invalid = ColisRequestDTO.builder()
                    .expediteurNom("Alice")
                    .expediteurAdresse("12 rue de Paris")
                    .expediteurEmail("alice@email.com")
                    .destinataireNom("Bob")
                    .destinataireAdresse("5 avenue de Lyon")
                    .optionService(OptionService.EXPRESS)
                    .build();

            mockMvc.perform(post("/api/colis")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_CLIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreurs.poids").exists());
        }

        @Test
        @DisplayName("avec email invalide → 400 Bad Request")
        void withInvalidEmail_shouldReturn400() throws Exception {
            ColisRequestDTO invalid = ColisRequestDTO.builder()
                    .expediteurNom("Alice")
                    .expediteurAdresse("12 rue de Paris")
                    .expediteurEmail("pas-un-email")
                    .destinataireNom("Bob")
                    .destinataireAdresse("5 avenue de Lyon")
                    .poids(2.5)
                    .optionService(OptionService.EXPRESS)
                    .build();

            mockMvc.perform(post("/api/colis")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_CLIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── GET /api/colis/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/colis/{id}")
    class GetColisTests {

        @Test
        @DisplayName("colis existant avec bon userId → 200 OK")
        void existingColis_shouldReturn200() throws Exception {
            when(colisService.getColisById(1L, 1L, "ROLE_CLIENT")).thenReturn(responseDTO);

            mockMvc.perform(get("/api/colis/1")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_CLIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.numeroSuivi").value("COL-20240315-A3F7K"));
        }

        @Test
        @DisplayName("colis inexistant → 404 Not Found")
        void nonExistentColis_shouldReturn404() throws Exception {
            when(colisService.getColisById(eq(99L), any(), any()))
                    .thenThrow(new ColisNotFoundException(99L));

            mockMvc.perform(get("/api/colis/99")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_CLIENT"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Colis introuvable"));
        }
    }

    // ─── GET /api/colis ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/colis")
    class GetAllColisTests {

        @Test
        @DisplayName("liste des colis → 200 OK")
        void shouldReturnListOf200() throws Exception {
            when(colisService.getAllColis(1L, "ROLE_CLIENT")).thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/colis")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_CLIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].numeroSuivi").value("COL-20240315-A3F7K"));
        }
    }

    // ─── PATCH /api/colis/{id}/statut ────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/colis/{id}/statut")
    class PatchStatutTests {

        @Test
        @DisplayName("transition valide → 200 OK")
        void validTransition_shouldReturn200() throws Exception {
            ColisResponseDTO updated = ColisResponseDTO.builder()
                    .id(1L)
                    .statut(StatutColis.ENLEVE)
                    .build();

            when(colisService.updateStatut(eq(1L), eq(StatutColis.ENLEVE), eq(1L), eq("ROLE_LIVREUR")))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/colis/1/statut")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_LIVREUR")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateStatutDTO(StatutColis.ENLEVE))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("ENLEVE"));
        }

        @Test
        @DisplayName("transition invalide → 400 Bad Request")
        void invalidTransition_shouldReturn400() throws Exception {
            when(colisService.updateStatut(eq(1L), eq(StatutColis.LIVRE), any(), any()))
                    .thenThrow(new InvalidStatutTransitionException(StatutColis.EN_ATTENTE, StatutColis.LIVRE));

            mockMvc.perform(patch("/api/colis/1/statut")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateStatutDTO(StatutColis.LIVRE))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Transition de statut invalide"));
        }
    }
}