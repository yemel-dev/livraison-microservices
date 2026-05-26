package com.livraison.colis.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre Zero Trust — s'exécute UNE SEULE FOIS par requête HTTP.
 *
 * Principe :
 * - Le colis-service ne voit JAMAIS de token JWT.
 * - L'api-gateway a déjà validé le JWT et injecte l'identité dans les headers.
 * - Ce filtre lit ces headers et construit le contexte de sécurité Spring.
 *
 * Flow :
 *   Requête entrante
 *     → Lire X-User-Id  (absent → 401)
 *     → Lire X-User-Role (absent → 401)
 *     → Créer UsernamePasswordAuthenticationToken
 *     → Placer dans SecurityContextHolder
 *     → Laisser passer la requête
 */
@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID   = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String userRole     = request.getHeader(HEADER_USER_ROLE);

        // ── Vérification de la présence des headers obligatoires ─────────────
        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.warn("Requête rejetée : header {} manquant - URI={}", HEADER_USER_ID, request.getRequestURI());
            sendUnauthorized(response, "Header " + HEADER_USER_ID + " manquant ou vide.");
            return;
        }

        if (userRole == null || userRole.isBlank()) {
            log.warn("Requête rejetée : header {} manquant - URI={}", HEADER_USER_ROLE, request.getRequestURI());
            sendUnauthorized(response, "Header " + HEADER_USER_ROLE + " manquant ou vide.");
            return;
        }

        // ── Parsing du userId ────────────────────────────────────────────────
        Long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("Requête rejetée : {} invalide (valeur='{}')", HEADER_USER_ID, userIdHeader);
            sendUnauthorized(response, "Header " + HEADER_USER_ID + " doit être un nombre entier.");
            return;
        }

        // ── Construction du contexte de sécurité Spring ──────────────────────
        // Le "principal" est le userId (Long), les "credentials" sont null (pas de mot de passe ici)
        // Les "authorities" contiennent le rôle extrait du header
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,                                          // principal = userId
                        null,                                            // credentials = null
                        List.of(new SimpleGrantedAuthority(userRole))   // authorities = rôle
                );

        // Placer l'authentification dans le SecurityContext
        // → Spring Security considère cet utilisateur comme authentifié pour toute la durée de la requête
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authentification Zero Trust établie : userId={}, role={}, URI={}",
                userId, userRole, request.getRequestURI());

        // Laisser passer la requête vers le controller
        filterChain.doFilter(request, response);

        // Nettoyage du SecurityContext après la requête (bonne pratique stateless)
        SecurityContextHolder.clearContext();
    }

    /**
     * Écrit une réponse 401 JSON directement dans la réponse HTTP.
     * Utilisé quand le filtre rejette la requête avant d'atteindre le controller.
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"status\":401,\"error\":\"Non authentifié\",\"message\":\"%s\"}", message
        ));
    }
}