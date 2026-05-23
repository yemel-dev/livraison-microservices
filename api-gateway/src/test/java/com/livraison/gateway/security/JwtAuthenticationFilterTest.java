package com.livraison.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        // Injecter les chemins publics — PAS de when(chain.filter()) ici
        // car certains tests ne font jamais appel à chain (tests 401)
        // et Mockito strict mode le refuse
        List<String> publicPaths = Arrays.asList(
                "/api/auth/login",
                "/api/auth/register",
                "/actuator/health"
        );
        ReflectionTestUtils.setField(filter, "publicPaths", publicPaths);
    }

    // ── Méthode utilitaire ── crée un exchange avec path et header optionnel
    private MockServerWebExchange creerExchange(String method, String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder =
                MockServerHttpRequest.method(
                        org.springframework.http.HttpMethod.valueOf(method), path);
        if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
        }
        return MockServerWebExchange.from(requestBuilder.build());
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 13 — Chemin public → passe sans vérifier le JWT
    // ─────────────────────────────────────────────────────────────────
    @Test
    void cheminPublic_passeDirectement() {
        // when() ici car ce test appelle chain.filter()
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerWebExchange exchange = creerExchange("GET", "/api/auth/login", null);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 14 — Sans header Authorization → 401
    // ─────────────────────────────────────────────────────────────────
    @Test
    void sansHeader_retourne401() {
        // PAS de when(chain.filter()) — ce test ne doit JAMAIS appeler chain
        MockServerWebExchange exchange = creerExchange("GET", "/api/colis", null);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 16 — Token invalide → 401
    // ─────────────────────────────────────────────────────────────────
    @Test
    void tokenInvalide_retourne401() {
        // PAS de when(chain.filter()) — ce test ne doit JAMAIS appeler chain
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        MockServerWebExchange exchange = creerExchange("GET", "/api/colis", "Bearer bad-token");

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 18 — Token valide → X-User-Id et X-User-Role injectés
    // ─────────────────────────────────────────────────────────────────
    @Test
    void tokenValide_headersInjectes() {
        // when() ici car ce test appelle chain.filter()
        when(chain.filter(any())).thenReturn(Mono.empty());
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn("user-42");
        when(jwtUtil.extractRole("valid-token")).thenReturn("CLIENT");

        MockServerWebExchange exchange = creerExchange("GET", "/api/colis", "Bearer valid-token");

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange enrichi = captor.getValue();
        assertEquals("user-42", enrichi.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("CLIENT",  enrichi.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST 20 — Réponse 401 → corps JSON bien formé
    // ─────────────────────────────────────────────────────────────────
    @Test
    void erreur401_bodyJson_bienFormate() {
        // PAS de when(chain.filter()) — ce test ne doit JAMAIS appeler chain
        MockServerWebExchange exchange = creerExchange("GET", "/api/colis", null);

        filter.filter(exchange, chain).block();

        String body = exchange.getResponse().getBodyAsString().block();

        assertNotNull(body,                  "Le corps ne doit pas être null");
        assertTrue(body.contains("401"),     "Le corps doit contenir 401");
        assertTrue(body.contains("message"), "Le corps doit contenir 'message'");
        assertTrue(body.contains("path"),    "Le corps doit contenir 'path'");
    }
}