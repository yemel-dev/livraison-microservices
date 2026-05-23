package com.livraison.gateway.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ErrorResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 1 — La méthode factory crée l'objet avec les bons champs
    // ─────────────────────────────────────────────────────────────────
    @Test
    void of_doitCreerAvecTimestamp() {
        ErrorResponse response = ErrorResponse.of(401, "Token manquant", "/api/colis");

        assertEquals(401, response.getStatus());
        assertEquals("Token manquant", response.getMessage());
        assertEquals("/api/colis", response.getPath());
        assertNotNull(response.getTimestamp());
        // Le timestamp doit être dans le passé immédiat (créé il y a moins d'1 seconde)
        assertTrue(response.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 2 — Le JSON généré contient tous les champs attendus
    // Le frontend lit ces champs pour afficher le message d'erreur
    // ─────────────────────────────────────────────────────────────────
    @Test
    void serialisationJson_doitContenirTousLesChamps() throws Exception {
        ErrorResponse response = ErrorResponse.of(401, "Token manquant", "/api/colis");

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("status"),    "Le JSON doit contenir 'status'");
        assertTrue(json.contains("message"),   "Le JSON doit contenir 'message'");
        assertTrue(json.contains("path"),      "Le JSON doit contenir 'path'");
        assertTrue(json.contains("timestamp"), "Le JSON doit contenir 'timestamp'");
        assertTrue(json.contains("401"),       "Le JSON doit contenir la valeur 401");
        assertTrue(json.contains("Token manquant"), "Le JSON doit contenir le message");
    }
}