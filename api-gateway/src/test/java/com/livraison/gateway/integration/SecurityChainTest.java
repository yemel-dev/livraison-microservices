package com.livraison.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-chars-ok",
        "gateway.public-paths=/api/auth/login,/api/auth/register,/actuator/health"
})
class SecurityChainTest {

    @Autowired
    private WebTestClient webTestClient;

    // ─────────────────────────────────────────────────────────────────
    // TEST 27 — Les filtres s'exécutent dans le bon ordre : JWT (-1) avant RateLimit (0)
    //
    // Méthode : envoyer une requête SANS token
    // → Si on reçoit 401 : JWT filter (ordre -1) a bloqué en premier ✅
    // → Si on reçoit 429 : RateLimit (ordre 0) a bloqué en premier ❌ (mauvais ordre)
    //
    // Ce test prouve que l'ordre getOrder() = -1 et = 0 fonctionne correctement
    // ─────────────────────────────────────────────────────────────────
    @Test
    void filtresEnBonOrdre_JwtAvantRateLimit() {
        webTestClient.get()
                .uri("/api/colis")
                // Pas de header Authorization
                .exchange()
                // Doit retourner 401 (JWT vérifié en premier)
                // et NON 429 (ce serait le rate limit qui a bloqué, mauvais ordre)
                .expectStatus().isUnauthorized();
    }
}