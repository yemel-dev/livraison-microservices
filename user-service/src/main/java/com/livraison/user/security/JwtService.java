package com.livraison.user.security;

import com.livraison.user.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Génère, valide et lit les tokens JWT.
 * Utilise la bibliothèque jjwt 0.12.5 — nouvelle API.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // en millisecondes — 86400000 = 24h

    // -------------------------------------------------------------------------
    // Génère un token JWT signé pour l'utilisateur connecté.
    // Le token contient : email (subject), userId, role, nom, dates.
    // -------------------------------------------------------------------------
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getEmail())             // Identifiant principal
                .claim("userId", user.getId())        // Extrait par le Gateway → X-User-Id
                .claim("role",   user.getRole().name()) // Extrait par le Gateway → X-User-Role
                .claim("nom",    user.getNom())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())             // Signature HMAC-SHA256
                .compact();
    }

    // -------------------------------------------------------------------------
    // Valide le token — retourne true si valide, false sinon.
    // Ne lève jamais d'exception — le filtre décide quoi faire avec false.
    // -------------------------------------------------------------------------
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) return false;
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Erreur inattendue validation JWT : {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Extrait l'email (subject) depuis le token.
    // -------------------------------------------------------------------------
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // -------------------------------------------------------------------------
    // Extrait le rôle depuis le claim "role".
    // -------------------------------------------------------------------------
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // -------------------------------------------------------------------------
    // Extrait l'userId depuis le claim "userId".
    // -------------------------------------------------------------------------
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    // -------------------------------------------------------------------------
    // Extrait tous les claims du token — utilisé en interne.
    // -------------------------------------------------------------------------
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // -------------------------------------------------------------------------
    // Construit la clé HMAC-SHA256 depuis le secret en application.yml.
    // -------------------------------------------------------------------------
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}