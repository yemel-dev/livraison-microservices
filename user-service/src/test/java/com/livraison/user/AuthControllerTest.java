package com.livraison.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.user.config.SecurityConfig;
import com.livraison.user.controller.AuthController;
import com.livraison.user.dto.*;
import com.livraison.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;
    @MockBean  private UserService   userService;

    private AuthResponse mockAuthResponse() {
        return AuthResponse.builder()
                .token("jwt-token-test")
                .userId(1L)
                .nom("Jean")
                .email("jean@test.com")
                .role("CLIENT")
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register corps valide → HTTP 201 + token")
    void register_corpsValide_retourne201() throws Exception {
        when(userService.register(any())).thenReturn(mockAuthResponse());

        RegisterRequest req = new RegisterRequest();
        req.setNom("Jean"); req.setPrenom("Test");
        req.setEmail("jean@test.com"); req.setMotDePasse("123456");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-test"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    @DisplayName("POST /register email vide → HTTP 400")
    void register_emailVide_retourne400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setNom("Jean"); req.setPrenom("Test");
        req.setEmail("");             // invalide
        req.setMotDePasse("123456");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login credentials valides → HTTP 200 + token")
    void login_credentialsValides_retourne200() throws Exception {
        when(userService.login(any())).thenReturn(mockAuthResponse());

        LoginRequest req = new LoginRequest();
        req.setEmail("jean@test.com");
        req.setMotDePasse("123456");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-test"));
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users/me avec header X-User-Id → HTTP 200")
    void getProfile_avecHeader_retourne200() throws Exception {
        UserDTO dto = UserDTO.builder()
                .id(1L).nom("Jean").prenom("Test")
                .email("jean@test.com").role("CLIENT")
                .build();

        when(userService.getProfile(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/auth/users/me")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jean@test.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    @DisplayName("GET /users/me sans header X-User-Id → HTTP 400")
    void getProfile_sansHeader_retourne400() throws Exception {
        mockMvc.perform(get("/api/auth/users/me"))
                .andExpect(status().isBadRequest());
    }
}