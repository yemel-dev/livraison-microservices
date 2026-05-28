package com.livraison.livreur.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.livreur.dto.AssignerLivraisonRequest;
import com.livraison.livreur.dto.EchecRequest;
import com.livraison.livreur.dto.LivraisonResponse;
import com.livraison.livreur.enums.StatutLivraison;
import com.livraison.livreur.security.SecurityContext;
import com.livraison.livreur.service.LivraisonService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LivraisonController.class)
@DisplayName("LivraisonController — Tests d'intégration (MockMvc)")
class LivraisonControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean LivraisonService livraisonService;

    private LivraisonResponse livraisonResponse;

    @BeforeEach
    void setUp() {
        livraisonResponse = LivraisonResponse.builder()
                .id(10L).numeroSuivi("TRK-2024-001")
                .livreurId(1L).livreurNom("Jean Dupont")
                .statut(StatutLivraison.ASSIGNEE)
                .dateAssignation(LocalDateTime.now())
                .build();
    }

    // ─── POST /api/livraisons ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/livraisons — retourne 201 si ADMIN et données valides")
    void assignerColis_admin_retourne201() throws Exception {
        AssignerLivraisonRequest req = new AssignerLivraisonRequest();
        req.setNumeroSuivi("TRK-2024-001"); req.setLivreurId(1L);

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(true);
            when(livraisonService.assignerColis(any())).thenReturn(livraisonResponse);

            mockMvc.perform(post("/api/livraisons")
                            .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statut").value("ASSIGNEE"))
                    .andExpect(jsonPath("$.numeroSuivi").value("TRK-2024-001"));
        }
    }

    @Test
    @DisplayName("POST /api/livraisons — retourne 403 si non ADMIN")
    void assignerColis_nonAdmin_retourne403() throws Exception {
        AssignerLivraisonRequest req = new AssignerLivraisonRequest();
        req.setNumeroSuivi("TRK-001"); req.setLivreurId(1L);

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(false);

            mockMvc.perform(post("/api/livraisons")
                            .header("X-User-Id", "5").header("X-User-Role", "LIVREUR")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /api/livreurs/{livreurId}/tournee ────────────────────────────────

    @Test
    @DisplayName("GET /api/livreurs/{id}/tournee — retourne 200 avec liste tournée")
    void getTourneeJour_retourne200() throws Exception {
        when(livraisonService.getTourneeJour(1L)).thenReturn(List.of(livraisonResponse));

        mockMvc.perform(get("/api/livreurs/1/tournee")
                        .header("X-User-Id", "1").header("X-User-Role", "LIVREUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroSuivi").value("TRK-2024-001"));
    }

    // ─── PATCH /api/livraisons/{id}/scanner/{livreurId} ──────────────────────

    @Test
    @DisplayName("PATCH scanner — retourne 200 avec statut ENLEVEE")
    void scanner_retourne200() throws Exception {
        LivraisonResponse enlevee = LivraisonResponse.builder()
                .id(10L).numeroSuivi("TRK-2024-001").livreurId(1L)
                .statut(StatutLivraison.ENLEVEE).dateAssignation(LocalDateTime.now()).build();

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(false);
            sc.when(SecurityContext::getCurrentUserId).thenReturn(1L);
            when(livraisonService.scannerPriseEnCharge(10L, 1L)).thenReturn(enlevee);

            mockMvc.perform(patch("/api/livraisons/10/scanner/1")
                            .header("X-User-Id", "1").header("X-User-Role", "LIVREUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("ENLEVEE"));
        }
    }

    // ─── PATCH /api/livraisons/{id}/confirmer/{livreurId} ────────────────────

    @Test
    @DisplayName("PATCH confirmer — retourne 200 avec statut LIVREE")
    void confirmer_retourne200() throws Exception {
        LivraisonResponse livree = LivraisonResponse.builder()
                .id(10L).numeroSuivi("TRK-2024-001").livreurId(1L)
                .statut(StatutLivraison.LIVREE).dateAssignation(LocalDateTime.now())
                .dateLivraison(LocalDateTime.now()).build();

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(false);
            sc.when(SecurityContext::getCurrentUserId).thenReturn(1L);
            when(livraisonService.confirmerLivraison(10L, 1L)).thenReturn(livree);

            mockMvc.perform(patch("/api/livraisons/10/confirmer/1")
                            .header("X-User-Id", "1").header("X-User-Role", "LIVREUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("LIVREE"))
                    .andExpect(jsonPath("$.dateLivraison").isNotEmpty());
        }
    }

    // ─── PATCH /api/livraisons/{id}/echec/{livreurId} ────────────────────────

    @Test
    @DisplayName("PATCH echec — retourne 200 avec statut ECHEC et motif")
    void echec_retourne200() throws Exception {
        EchecRequest req = new EchecRequest();
        req.setMotifEchec("Destinataire absent");

        LivraisonResponse echec = LivraisonResponse.builder()
                .id(10L).numeroSuivi("TRK-2024-001").livreurId(1L)
                .statut(StatutLivraison.ECHEC).motifEchec("Destinataire absent")
                .dateAssignation(LocalDateTime.now()).build();

        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(false);
            sc.when(SecurityContext::getCurrentUserId).thenReturn(1L);
            when(livraisonService.enregistrerEchec(eq(10L), eq(1L), any())).thenReturn(echec);

            mockMvc.perform(patch("/api/livraisons/10/echec/1")
                            .header("X-User-Id", "1").header("X-User-Role", "LIVREUR")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("ECHEC"))
                    .andExpect(jsonPath("$.motifEchec").value("Destinataire absent"));
        }
    }

    @Test
    @DisplayName("PATCH echec — retourne 400 si motif absent")
    void echec_motifAbsent_retourne400() throws Exception {
        try (MockedStatic<SecurityContext> sc = mockStatic(SecurityContext.class)) {
            sc.when(SecurityContext::isAdmin).thenReturn(false);
            sc.when(SecurityContext::getCurrentUserId).thenReturn(1L);

            mockMvc.perform(patch("/api/livraisons/10/echec/1")
                            .header("X-User-Id", "1").header("X-User-Role", "LIVREUR")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
