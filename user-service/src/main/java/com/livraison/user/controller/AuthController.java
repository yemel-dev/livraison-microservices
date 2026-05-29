package com.livraison.user.controller;

import com.livraison.user.dto.*;
import com.livraison.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoints REST d'authentification.
 * Base path : /api/auth
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Inscription, connexion et profil utilisateur")
public class AuthController {

    private final UserService userService;

    // -------------------------------------------------------------------------
    // POST /api/auth/register → HTTP 201
    // -------------------------------------------------------------------------
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscrire un nouvel utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Utilisateur créé — token JWT retourné"),
        @ApiResponse(responseCode = "400", description = "Email déjà utilisé ou données invalides")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login → HTTP 200
    // -------------------------------------------------------------------------
    @PostMapping("/login")
    @Operation(summary = "Se connecter — retourne un token JWT valide 24h")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token JWT retourné"),
        @ApiResponse(responseCode = "400", description = "Email ou mot de passe incorrect")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/users/me → HTTP 200
    // Le Gateway injecte X-User-Id depuis le token JWT — pas besoin de re-valider
    // -------------------------------------------------------------------------
    @GetMapping("/users/me")
    @Operation(summary = "Profil de l'utilisateur connecté — JWT requis via Gateway")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil retourné sans mot de passe"),
        @ApiResponse(responseCode = "400", description = "Header X-User-Id manquant")
    })
    public UserDTO getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        return userService.getProfile(userId);
    }

    // -------------------------------------------------------------------------
    // Gestionnaire d'erreurs — toutes les RuntimeException → HTTP 400
    // -------------------------------------------------------------------------
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleRuntimeException(RuntimeException ex) {
        return Map.of(
            "error",     ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        );
    }
}