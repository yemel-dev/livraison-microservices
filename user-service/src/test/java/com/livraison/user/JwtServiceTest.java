package com.livraison.user;

import com.livraison.user.model.Role;
import com.livraison.user.model.User;
import com.livraison.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-32-chars-minimum-ok");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        testUser = User.builder()
                .id(1L)
                .nom("Jean")
                .prenom("Test")
                .email("jean@test.com")
                .motDePasse("hashed")
                .role(Role.CLIENT)
                .build();
    }

    @Test
    @DisplayName("generateToken — retourne un token non null commençant par eyJ")
    void generateToken_retourneTokenValide() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"),
                "Un token JWT doit commencer par eyJ");
    }

    @Test
    @DisplayName("validateToken — retourne true pour un token valide")
    void validateToken_tokenValide_retourneTrue() {
        String token = jwtService.generateToken(testUser);
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    @DisplayName("validateToken — retourne false pour un token invalide")
    void validateToken_tokenInvalide_retourneFalse() {
        assertFalse(jwtService.validateToken("token.invalide.xxx"));
    }

    @Test
    @DisplayName("validateToken — retourne false pour null")
    void validateToken_null_retourneFalse() {
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    @DisplayName("extractEmail — retourne le bon email")
    void extractEmail_retourneBonEmail() {
        String token = jwtService.generateToken(testUser);
        assertEquals("jean@test.com", jwtService.extractEmail(token));
    }

    @Test
    @DisplayName("extractRole — retourne le bon rôle")
    void extractRole_retourneBonRole() {
        String token = jwtService.generateToken(testUser);
        assertEquals("CLIENT", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("extractUserId — retourne le bon userId")
    void extractUserId_retourneBonId() {
        String token = jwtService.generateToken(testUser);
        assertEquals(1L, jwtService.extractUserId(token));
    }
}