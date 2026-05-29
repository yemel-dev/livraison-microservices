package com.livraison.livreur.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre Zero Trust — Double barrière de sécurité.
 *
 * L'API Gateway valide le JWT et injecte les headers X-User-Id et X-User-Role.
 * Ce filtre vérifie que ces headers sont bien présents avant d'autoriser la requête.
 * Même si la requête vient d'un service interne, elle DOIT avoir ces headers.
 */
@Component
@Slf4j
public class ZeroTrustFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    // Chemins publics — pas besoin de token JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Laisser passer les chemins publics sans vérification
        if (isPublicPath(uri)) {
            log.debug("[ZERO TRUST] Chemin public autorisé sans token : {}", uri);
            chain.doFilter(request, response);
            return;
        }

        String userId   = request.getHeader(HEADER_USER_ID);
        String userRole = request.getHeader(HEADER_USER_ROLE);

        if (userId == null || userRole == null) {
            log.warn("[ZERO TRUST] Requête refusée — headers manquants | URI={} | IP={}",
                    uri, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"message\":\"Accès non autorisé — token JWT manquant\"}"
            );
            return;
        }

        log.debug("[ZERO TRUST] Requête autorisée | userId={} | role={} | URI={}",
                userId, userRole, uri);

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String uri) {
        return PUBLIC_PATHS.stream().anyMatch(uri::startsWith);
    }
}