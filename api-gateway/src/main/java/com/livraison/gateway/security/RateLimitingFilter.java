package com.livraison.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livraison.gateway.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    // -------------------------------------------------------------------------
    // CONSTANTES — fenêtre glissante de 1 minute, max 100 requêtes par IP
    // -------------------------------------------------------------------------
    private static final int  MAX_REQUESTS   = 100;
    private static final long WINDOW_MILLIS  = 60_000L;  // 1 minute en ms

    // -------------------------------------------------------------------------
    // STOCKAGE EN MÉMOIRE — thread-safe grâce à ConcurrentHashMap
    // Clé   : adresse IP (String)
    // Valeur : liste des timestamps (ms) des requêtes dans la fenêtre courante
    // -------------------------------------------------------------------------
    private final ConcurrentHashMap<String, List<Long>> requestCounts
            = new ConcurrentHashMap<>();

    // ObjectMapper pour sérialiser ErrorResponse en JSON
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -------------------------------------------------------------------------
    // ORDRE 0 : s'exécute APRÈS JwtAuthenticationFilter (ordre -1)
    // Une requête qui arrive ici a déjà été validée par le filtre JWT
    // -------------------------------------------------------------------------
    @Override
    public int getOrder() {
        return 0;
    }

    // -------------------------------------------------------------------------
    // FILTRE PRINCIPAL
    // -------------------------------------------------------------------------
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // ÉTAPE 1 — Extraire l'adresse IP du client
        // getRemoteAddress() peut être null dans certains environnements de test
        String ip = extractIp(exchange);

        String path = exchange.getRequest().getURI().getPath();

        // ÉTAPE 2 — Vérifier si cette IP a dépassé la limite
        if (isRateLimited(ip)) {
            log.warn("Rate limit dépassé | ip={} | path={} | limite={} req/min",
                    ip, path, MAX_REQUESTS);
            return sendError(exchange, 429, "Trop de requêtes — réessayez dans une minute", path);
        }

        // ÉTAPE 3 — IP dans les limites : laisser passer
        return chain.filter(exchange);
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Algorithme de la fenêtre glissante
    //
    // Pour chaque IP :
    //   1. Récupérer (ou créer) sa liste de timestamps
    //   2. Supprimer les timestamps plus vieux que (maintenant - 60s)
    //      → c'est le "nettoyage" intégré, sans @Scheduled
    //   3. Ajouter le timestamp de la requête courante
    //   4. Retourner true si le nombre de timestamps dépasse MAX_REQUESTS
    //
    // computeIfAbsent() est thread-safe : crée la liste uniquement si absente
    // -------------------------------------------------------------------------
    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();

        List<Long> timestamps = requestCounts.computeIfAbsent(ip,
                k -> new ArrayList<>());

        synchronized (timestamps) {
            // Supprimer les requêtes hors de la fenêtre glissante
            timestamps.removeIf(t -> t < now - WINDOW_MILLIS);

            // Ajouter la requête courante
            timestamps.add(now);

            // Vérifier si la limite est dépassée
            return timestamps.size() > MAX_REQUESTS;
        }
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Extraire l'adresse IP
    // Priorité 1 : header X-Forwarded-For (présent derrière un proxy/load balancer)
    // Priorité 2 : adresse IP directe de la connexion TCP
    // Fallback    : "unknown" si tout est null (cas des tests unitaires)
    // -------------------------------------------------------------------------
    private String extractIp(ServerWebExchange exchange) {
        // Derrière un reverse proxy (Nginx, K8s Ingress), l'IP réelle est dans ce header
        String forwardedFor = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For peut contenir plusieurs IPs séparées par virgule
            // La première est toujours l'IP du client original
            return forwardedFor.split(",")[0].trim();
        }

        // Connexion directe (développement local)
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRIVÉE — Envoyer une réponse d'erreur JSON
    // Identique à JwtAuthenticationFilter.sendError()
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

        // CORRECTION : cast explicite pour satisfaire les annotations @NonNull
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(bytes);

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));   // ← Mono.just au lieu de Flux.just

    } catch (Exception e) {
        log.error("Erreur lors de la sérialisation de ErrorResponse", e);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return exchange.getResponse().setComplete();
    }
}
}