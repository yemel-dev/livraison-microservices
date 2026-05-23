package com.livraison.gateway.integration;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// @SpringBootTest démarre le contexte Spring complet (pas de mocks)
// RANDOM_PORT évite les conflits de port avec d'autres tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-chars-ok",
        "gateway.public-paths=/api/auth/login,/api/auth/register,/actuator/health"
})
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String TEST_SECRET = "test-secret-key-minimum-32-chars-ok";

    // ── Méthode utilitaire ── crée un JWT valide avec la clé de test
    private String creerTokenValide() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user-test-42")
                .claim("role", "CLIENT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 26 — Flux complet : JWT valide → Gateway accepte et tente le routage
    //
    // Résultat attendu : PAS 401 (Zero Trust passé avec succès)
    // On accepte 502 (service colis absent) car cela prouve que le Gateway
    // a validé le token ET tenté de router vers :8082
    // ─────────────────────────────────────────────────────────────────
    @Test
    void fluxComplet_tokenValide_routageOk() {
        webTestClient.get()
                .uri("/api/colis")
                .header("Authorization", "Bearer " + creerTokenValide())
                .exchange()
                // Ne doit PAS être 401 — le Zero Trust est passé
                // 502 = Gateway a essayé de router mais colis-service n'est pas démarré
                .expectStatus().value(status ->
                        assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                "Un token valide ne doit JAMAIS retourner 401"));
    }
}