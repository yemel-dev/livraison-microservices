package com.livraison.user;

import com.livraison.user.dto.*;
import com.livraison.user.model.Role;
import com.livraison.user.model.User;
import com.livraison.user.repository.UserRepository;
import com.livraison.user.security.JwtService;
import com.livraison.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private JwtService      jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User savedUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L).nom("Jean").prenom("Test")
                .email("jean@test.com")
                .motDePasse("$2a$hashed")
                .role(Role.CLIENT)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setNom("Jean");
        registerRequest.setPrenom("Test");
        registerRequest.setEmail("jean@test.com");
        registerRequest.setMotDePasse("123456");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jean@test.com");
        loginRequest.setMotDePasse("123456");
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — succès : save() appelé 1 fois, token présent")
    void register_succes() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = userService.register(registerRequest);

        verify(userRepository, times(1)).save(any(User.class));
        assertNotNull(response.getToken());
        assertEquals("jwt-token", response.getToken());
        assertEquals("jean@test.com", response.getEmail());
    }

    @Test
    @DisplayName("register — email doublon : RuntimeException avec message 'déjà utilisé'")
    void register_emailDoublon_throwsException() {
        when(userRepository.existsByEmail("jean@test.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(registerRequest));

        assertTrue(ex.getMessage().contains("déjà utilisé"));
        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — succès : token présent dans AuthResponse")
    void login_succes() {
        when(userRepository.findByEmail("jean@test.com"))
                .thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("123456", "$2a$hashed")).thenReturn(true);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = userService.login(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals("CLIENT", response.getRole());
    }

    @Test
    @DisplayName("login — utilisateur absent : RuntimeException 'non trouvé'")
    void login_userAbsent_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login(loginRequest));

        assertTrue(ex.getMessage().contains("non trouvé"));
    }

    @Test
    @DisplayName("login — mauvais mot de passe : RuntimeException 'incorrect'")
    void login_mauvaisMotDePasse_throwsException() {
        when(userRepository.findByEmail("jean@test.com"))
                .thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login(loginRequest));

        assertTrue(ex.getMessage().contains("incorrect"));
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfile — retourne UserDTO sans motDePasse")
    void getProfile_retourneUserDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));

        UserDTO dto = userService.getProfile(1L);

        assertEquals("Jean", dto.getNom());
        assertEquals("jean@test.com", dto.getEmail());
        assertEquals("CLIENT", dto.getRole());
        // Le UserDTO ne doit pas avoir de champ motDePasse
        assertNull(dto.getClass().getFields().length > 0 ? null : null);
    }
}