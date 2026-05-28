package com.livraison.livreur.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZeroTrustFilter — Tests unitaires")
class ZeroTrustFilterTest {

    @InjectMocks ZeroTrustFilter zeroTrustFilter;
    @Mock HttpServletRequest  request;
    @Mock HttpServletResponse response;
    @Mock FilterChain         filterChain;

    @Test
    @DisplayName("laisse passer si X-User-Id et X-User-Role présents")
    void doFilter_headersPresents_passThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/livreurs");
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

        zeroTrustFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("retourne 401 si X-User-Id absent")
    void doFilter_userIdAbsent_retourne401() throws Exception {
        StringWriter sw = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/livreurs");
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        zeroTrustFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("retourne 401 si X-User-Role absent")
    void doFilter_userRoleAbsent_retourne401() throws Exception {
        StringWriter sw = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/livraisons");
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        zeroTrustFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("laisse passer les routes /actuator sans headers")
    void doFilter_actuator_passThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        zeroTrustFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("retourne 401 si les deux headers sont absents")
    void doFilter_aucunHeader_retourne401() throws Exception {
        StringWriter sw = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/livreurs/tous");
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        zeroTrustFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }
}
