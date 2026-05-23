package com.livraison.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    // Clé de test — même longueur que la clé de prod (32+ chars)
    private static final String TEST_SECRET = "test-secret-key-minimum-32-chars-ok";

    @BeforeEach
    void setUp() {
        // Injecter le secret dans le champ privé via ReflectionTestUtils
        // (remplace le @Value("${jwt.secret}") qui ne fonctionne pas dans les tests unitaires)
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    }

    // ── Méthode utilitaire ── crée un token JWT pour les tests
    private String creerToken(String userId, String role, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 4 — Token valide et non expiré → true
    // ─────────────────────────────────────────────────────────────────
    @Test
    void validateToken_tokenValide_retourneTrue() {
        String token = creerToken("user-123", "CLIENT", 3_600_000L); // expire dans 1h
        assertTrue(jwtUtil.validateToken(token));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 5 — Token expiré → false SANS lever d'exception
    // ─────────────────────────────────────────────────────────────────
    @Test
    void validateToken_tokenExpire_retourneFalse() {
        String token = creerToken("user-123", "CLIENT", -1_000L); // déjà expiré
        // assertDoesNotThrow garantit qu'aucune exception n'est levée
        assertDoesNotThrow(() -> assertFalse(jwtUtil.validateToken(token)));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 6 — Token avec signature falsifiée → false
    // Simule un attaquant qui modifie le token
    // ─────────────────────────────────────────────────────────────────
    @Test
    void validateToken_signatureInvalide_retourneFalse() {
        String token = creerToken("user-123", "CLIENT", 3_600_000L);
        String tokenFalsifie = token + "tampered"; // signature corrompue
        assertFalse(jwtUtil.validateToken(tokenFalsifie));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 7 — Attaque alg=none — token sans signature → false
    // Un attaquant construit un token sans signer pour bypass la validation
    // ─────────────────────────────────────────────────────────────────
    @Test
    void validateToken_algorithmNone_retourneFalse() {
        // Construit manuellement un token avec alg=none (pas de signature)
        // Format JWT : base64(header).base64(payload). (sans signature)
        String headerBase64  = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payloadBase64 = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"hacker\",\"role\":\"ADMIN\"}".getBytes());
        String tokenAlgNone  = headerBase64 + "." + payloadBase64 + ".";

        assertFalse(jwtUtil.validateToken(tokenAlgNone));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 8 — Token null → false SANS NullPointerException
    // ─────────────────────────────────────────────────────────────────
    @Test
    void validateToken_tokenNull_retourneFalse() {
        assertDoesNotThrow(() -> assertFalse(jwtUtil.validateToken(null)));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 10 — extractUserId retourne bien le userId encodé dans le token
    // ─────────────────────────────────────────────────────────────────
    @Test
    void extractUserId_doitRetournerBonId() {
        String token = creerToken("user-42", "LIVREUR", 3_600_000L);
        assertEquals("user-42", jwtUtil.extractUserId(token));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 11 — extractRole retourne le bon rôle pour CLIENT, LIVREUR, ADMIN
    // ─────────────────────────────────────────────────────────────────
    @Test
    void extractRole_doitRetournerBonRole() {
        for (String role : new String[]{"CLIENT", "LIVREUR", "ADMIN"}) {
            String token = creerToken("user-1", role, 3_600_000L);
            assertEquals(role, jwtUtil.extractRole(token),
                    "Le rôle " + role + " doit être extrait correctement");
        }
    }
}