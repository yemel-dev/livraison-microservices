package com.livraison.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // -------------------------------------------------------------------------
    // MÉTHODE 1 — Valider un token
    // Retourne TOUJOURS false en cas de problème, jamais d'exception levée
    // C'est le filtre qui décide quoi faire avec false (renvoyer 401)
    // -------------------------------------------------------------------------
    public boolean validateToken(String token) {

        // Cas null ou vide : on refuse immédiatement sans aller plus loin
        if (token == null || token.trim().isEmpty()) {
            log.warn("Validation JWT échouée : token null ou vide");
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Validation JWT échouée : token expiré — {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Validation JWT échouée : signature invalide — {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Validation JWT échouée : token malformé — {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Validation JWT échouée : algorithme non supporté — {}", e.getMessage());
        } catch (JwtException e) {
            // Couvre tous les autres cas JWT (y compris l'attaque alg=none)
            log.warn("Validation JWT échouée : erreur JWT générale — {}", e.getMessage());
        } catch (Exception e) {
            // Filet de sécurité pour tout le reste (IllegalArgumentException, etc.)
            log.warn("Validation JWT échouée : erreur inattendue — {}", e.getMessage());
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // MÉTHODE 2 — Extraire l'identifiant de l'utilisateur (claim "sub")
    // C'est le User Service qui place l'userId dans le claim "sub" à la création
    // -------------------------------------------------------------------------
    public String extractUserId(String token) {
    return extractAllClaims(token).get("userId", Long.class).toString();

    }

    // -------------------------------------------------------------------------
    // MÉTHODE 3 — Extraire le rôle (claim "role")
    // Valeurs possibles : CLIENT, LIVREUR, ADMIN
    // Le filtre injecte ensuite ce rôle dans le header X-User-Role
    // -------------------------------------------------------------------------
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // -------------------------------------------------------------------------
    // MÉTHODE 4 — Extraire tous les claims du token
    // Utilisée en interne par extractUserId() et extractRole()
    // -------------------------------------------------------------------------
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Construire la clé de signature HMAC-SHA256
    // La clé est lue depuis application.yml (variable ${jwt.secret})
    // JAMAIS codée en dur dans le code source
    // -------------------------------------------------------------------------
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}