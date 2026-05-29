package com.livraison.livreur.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.livreur.dto.CreateLivreurRequest;
import com.livraison.livreur.dto.LivreurResponse;
import com.livraison.livreur.dto.UpdateLivreurRequest;
import com.livraison.livreur.exception.AccessDeniedException;
import com.livraison.livreur.exception.ResourceNotFoundException;
import com.livraison.livreur.security.SecurityContext;
import com.livraison.livreur.service.LivreurService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LivreurController.class)
@DisplayName("LivreurController — Tests d'intégration (MockMvc)")
class LivreurControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean LivreurService livreurService;

    private LivreurResponse livreurResponse;

    @BeforeEach
    void setUp() {
        livreurResponse = LivreurResponse.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Moto").actif(true)
                .build();
    }

    // ─── GET /api/livreurs ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/livreurs — retourne 200 avec liste des actifs")
    void getLivreursActifs_retourne200() throws Exception {
        when(livreurService.listerLivreursActifs()).thenReturn(List.of(livreurResponse));

        mockMvc.perform(get("/api/livreurs")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "LIVREUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Dupont"))
                .andExpect(jsonPath("$[0].actif").value(true));
    }

    // ─── GET /api/livreurs/tous ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/livreurs/tous — retourne 200 pour ADMIN")
    void getTousLivreurs_admin_retourne200() throws Exception {
        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);
            when(livreurService.listerTousLivreurs()).thenReturn(List.of(livreurResponse));

            mockMvc.perform(get("/api/livreurs/tous")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Test
    @DisplayName("GET /api/livreurs/tous — retourne 403 si non ADMIN")
    void getTousLivreurs_nonAdmin_retourne403() throws Exception {
        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(false);

            mockMvc.perform(get("/api/livreurs/tous")
                            .header("X-User-Id", "5")
                            .header("X-User-Role", "LIVREUR"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── POST /api/livreurs ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/livreurs — retourne 201 si ADMIN et données valides")
    void creerLivreur_admin_retourne201() throws Exception {
        CreateLivreurRequest req = CreateLivreurRequest.builder()
                .nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Moto").build();

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);
            when(livreurService.creerLivreur(any())).thenReturn(livreurResponse);

            mockMvc.perform(post("/api/livreurs")
                            .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nom").value("Dupont"));
        }
    }

    @Test
    @DisplayName("POST /api/livreurs — retourne 400 si champs manquants")
    void creerLivreur_champsManquants_retourne400() throws Exception {
        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);

            mockMvc.perform(post("/api/livreurs")
                            .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── PUT /api/livreurs/{id} ───────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/livreurs/{id} — retourne 200 si modification réussie")
    void mettreAJour_retourne200() throws Exception {
        UpdateLivreurRequest req = new UpdateLivreurRequest();
        req.setVehicule("Camion");

        LivreurResponse updated = LivreurResponse.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Camion").actif(true).build();

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);
            when(livreurService.mettreAJourLivreur(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/livreurs/1")
                            .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vehicule").value("Camion"));
        }
    }

    // ─── PATCH /api/livreurs/{id}/toggle ─────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/livreurs/{id}/toggle — retourne 200 avec actif inversé")
    void toggleActif_retourne200() throws Exception {
        LivreurResponse toggled = LivreurResponse.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .telephone("+237600000001").vehicule("Moto").actif(false).build();

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);
            when(livreurService.toggleActif(1L)).thenReturn(toggled);

            mockMvc.perform(patch("/api/livreurs/1/toggle")
                            .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actif").value(false));
        }
    }

    // ─── DELETE /api/livreurs/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/livreurs/{id} — retourne 204 pour ADMIN")
    void supprimer_admin_retourne204() throws Exception {
        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);
            doNothing().when(livreurService).supprimerLivreur(1L);

            mockMvc.perform(delete("/api/livreurs/1")
                            .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("GET /api/livreurs/{id} — retourne 404 si livreur inexistant")
    void getLivreur_inexistant_retourne404() throws Exception {
        when(livreurService.getLivreurById(99L))
                .thenThrow(new ResourceNotFoundException("Livreur non trouvé : 99"));

        mockMvc.perform(get("/api/livreurs/99")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }
}
