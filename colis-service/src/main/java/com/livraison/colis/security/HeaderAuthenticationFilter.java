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

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID   = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    // Endpoints publics qui ne nécessitent pas d'authentification
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator/health",
        "/actuator/info"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Laisser passer les endpoints publics sans vérification
        if (PUBLIC_PATHS.stream().anyMatch(uri::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String userRole     = request.getHeader(HEADER_USER_ROLE);

        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.warn("Requête rejetée : header {} manquant - URI={}", HEADER_USER_ID, uri);
            sendUnauthorized(response, "Header " + HEADER_USER_ID + " manquant ou vide.");
            return;
        }

        if (userRole == null || userRole.isBlank()) {
            log.warn("Requête rejetée : header {} manquant - URI={}", HEADER_USER_ROLE, uri);
            sendUnauthorized(response, "Header " + HEADER_USER_ROLE + " manquant ou vide.");
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("Requête rejetée : {} invalide (valeur='{}')", HEADER_USER_ID, userIdHeader);
            sendUnauthorized(response, "Header " + HEADER_USER_ID + " doit être un nombre entier.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(userRole))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Auth Zero Trust : userId={}, role={}, URI={}", userId, userRole, uri);

        filterChain.doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"status\":401,\"error\":\"Non authentifié\",\"message\":\"%s\"}", message
        ));
    }
}