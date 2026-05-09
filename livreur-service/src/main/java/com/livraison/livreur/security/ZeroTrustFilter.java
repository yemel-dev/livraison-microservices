package com.livraison.livreur.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * Filtre Zero Trust — Double barrière de sécurité.
 *
 * L'API Gateway valide le JWT et injecte les headers X-User-Id et X-User-Role.
 * Ce filtre vérifie que ces headers sont bien présents avant d'autoriser la requête.
 * Même si la requête vient d'un service interne, elle DOIT avoir ces headers.
 */

/** en gros :   c'est un filtre HTTP (OncePerRequestFilter) qui vérifie que chaque
 * requête contient bien les headers X-User-Id et X-User-Role. Si ces headers sont absents,
 *il renvoie immédiatement un HTTP 401 sans même aller dans le controller
 */
@Component
@Slf4j
public class ZeroTrustFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String userId   = request.getHeader(HEADER_USER_ID);
        String userRole = request.getHeader(HEADER_USER_ROLE);

        // Actuator / health check : on laisse passer sans vérification
        if (request.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        if (userId == null || userRole == null) {
            log.warn("[ZERO TRUST] Requête refusée — headers manquants | URI={} | IP={}",
                    request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"message\":\"Accès non autorisé — token JWT manquant\"}"
            );
            return;
        }

        log.debug("[ZERO TRUST] Requête autorisée | userId={} | role={} | URI={}",
                userId, userRole, request.getRequestURI());

        chain.doFilter(request, response);
    }
}
