package com.livraison.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // ── Méthode utilitaire ── crée un exchange avec une IP spécifique
    private MockServerWebExchange creerExchangeAvecIp(String ip) {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/colis")
                .remoteAddress(new InetSocketAddress(ip, 12345))
                .build();
        return MockServerWebExchange.from(request);
    }

    // ── Méthode utilitaire ── envoie N requêtes depuis une IP
    private void envoyerRequetes(String ip, int nombre) {
        for (int i = 0; i < nombre; i++) {
            filter.filter(creerExchangeAvecIp(ip), chain).block();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 22 — La 100e requête passe encore (frontière exacte)
    // Vérifie que la limite est bien 100 et non 99
    // ─────────────────────────────────────────────────────────────────
    @Test
    void exactementLimite_requetePassee() {
        String ip = "10.0.0.2";
        envoyerRequetes(ip, 99);

        // La 100e requête doit passer
        MockServerWebExchange exchange100 = creerExchangeAvecIp(ip);
        filter.filter(exchange100, chain).block();

        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS,
                exchange100.getResponse().getStatusCode(),
                "La 100e requête ne doit PAS être bloquée");
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 23 — La 101e requête retourne 429
    // Cœur du rate limiting — doit absolument fonctionner
    // ─────────────────────────────────────────────────────────────────
    @Test
    void depasse101_retourne429() {
        String ip = "10.0.0.3";
        envoyerRequetes(ip, 100);

        // La 101e doit être bloquée
        MockServerWebExchange exchange101 = creerExchangeAvecIp(ip);
        filter.filter(exchange101, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                exchange101.getResponse().getStatusCode(),
                "La 101e requête DOIT retourner 429");
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 25 — Deux IPs différentes ont des compteurs séparés
    // IP1 bloquée ne doit pas affecter IP2
    // ─────────────────────────────────────────────────────────────────
    @Test
    void ipsDistinctes_compteursSepares() {
        String ip1 = "10.0.0.5";
        String ip2 = "10.0.0.6";

        // Bloquer IP1 en dépassant la limite
        envoyerRequetes(ip1, 101);
        MockServerWebExchange exchange101 = creerExchangeAvecIp(ip1);
        filter.filter(exchange101, chain).block();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                exchange101.getResponse().getStatusCode(),
                "IP1 doit être bloquée après 101 requêtes");

        // IP2 avec seulement 1 requête doit passer librement
        MockServerWebExchange exchangeIp2 = creerExchangeAvecIp(ip2);
        filter.filter(exchangeIp2, chain).block();
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS,
                exchangeIp2.getResponse().getStatusCode(),
                "IP2 ne doit PAS être affectée par le blocage de IP1");
    }
}