package com.livraison.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livraison.gateway.model.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    // JwtUtil est injecté par le constructeur grâce à @RequiredArgsConstructor
    private final JwtUtil jwtUtil;

    // Liste des chemins qui ne nécessitent PAS de token JWT
    // Lue depuis application.yml : gateway.public-paths
    @Value("${gateway.public-paths}")
    private List<String> publicPaths;

    // ObjectMapper pour sérialiser ErrorResponse en JSON dans les réponses d'erreur
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -------------------------------------------------------------------------
    // ORDRE -1 : ce filtre s'exécute EN PREMIER, avant tous les autres
    // RateLimitingFilter sera à 0, donc après celui-ci
    // -------------------------------------------------------------------------
    @Override
    public int getOrder() {
        return -1;
    }

    // -------------------------------------------------------------------------
    // FILTRE PRINCIPAL — algorithme Zero Trust en 11 étapes
    // ServerWebExchange = objet WebFlux représentant requête + réponse
    // Retourne Mono<Void> car Spring Cloud Gateway est réactif (non-bloquant)
    // -------------------------------------------------------------------------
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // ÉTAPE 1-2 — Vérifier si le chemin est public
        // Ex : /api/auth/login, /api/auth/register, /actuator/health
        if (isPublicPath(path)) {
            log.debug("Chemin public autorisé sans JWT : {}", path);
            return chain.filter(exchange);
        }

        // ÉTAPE 3-4 — Récupérer et vérifier le header Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Zero Trust — accès refusé : header Authorization manquant ou malformé | path={}", path);
            return sendError(exchange, 401, "Token JWT manquant", path);
        }

        // ÉTAPE 5 — Extraire le token en supprimant "Bearer " (7 caractères)
        String token = authHeader.substring(7);

        // ÉTAPE 6 — Valider le token via JwtUtil
        if (!jwtUtil.validateToken(token)) {
            log.warn("Zero Trust — accès refusé : token JWT invalide ou expiré | path={}", path);
            return sendError(exchange, 401, "Token JWT invalide ou expiré", path);
        }

        // ÉTAPE 7-8 — Extraire les informations de l'utilisateur depuis le token
        String userId = jwtUtil.extractUserId(token);
        String role   = jwtUtil.extractRole(token);

        // ÉTAPE 9 — Logger l'accès accepté (traçabilité Zero Trust)
        log.debug("Zero Trust — accès autorisé | userId={} | role={} | path={}", userId, role, path);

        // ÉTAPE 10-11 — Injecter les headers enrichis et transmettre à la chaîne
        // Les services en aval (User, Colis, Livreur) liront X-User-Id et X-User-Role
        // sans avoir à re-valider le token eux-mêmes
        ServerWebExchange enrichedExchange = injectHeaders(exchange, userId, role);
        return chain.filter(enrichedExchange);
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Vérifier si un chemin est dans la liste publique
    // Utilise startsWith pour couvrir les sous-chemins :
    //   /api/auth/login   → couvert par /api/auth/login
    //   /api/auth/register → couvert par /api/auth/register
    // -------------------------------------------------------------------------
    private boolean isPublicPath(String path) {
        return publicPaths.stream()
                .anyMatch(path::startsWith);
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Injecter X-User-Id et X-User-Role dans la requête
    // exchange.mutate() crée une copie modifiée (immuabilité WebFlux)
    // Les services en aval reçoivent ces headers sans toucher au token JWT
    // -------------------------------------------------------------------------
    private ServerWebExchange injectHeaders(ServerWebExchange exchange,
                                            String userId,
                                            String role) {
        ServerHttpRequest enrichedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

        return exchange.mutate()
                .request(enrichedRequest)
                .build();
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Envoyer une réponse d'erreur JSON
    // Utilise DataBufferFactory de Spring WebFlux (pas HttpServletResponse)
    // Format de la réponse : ErrorResponse sérialisé en JSON
    // -------------------------------------------------------------------------
    private Mono<Void> sendError(ServerWebExchange exchange,
                                 int status,
                                 String message,
                                 String path) {
        try {
            ErrorResponse errorResponse = ErrorResponse.of(status, message, path);
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);

            exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
            exchange.getResponse().getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);

            return exchange.getResponse()
                    .writeWith(Flux.just(buffer));

        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation de ErrorResponse", e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }
}