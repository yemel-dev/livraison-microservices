package com.livraison.user.service;

import com.livraison.user.dto.*;
import com.livraison.user.model.Role;
import com.livraison.user.model.User;
import com.livraison.user.repository.UserRepository;
import com.livraison.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Logique métier du User Service.
 * Coordonne UserRepository, JwtService et PasswordEncoder.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final JwtService      jwtService;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // INSCRIPTION — vérifie email unique, hache le mot de passe, génère le token
    // -------------------------------------------------------------------------
    public AuthResponse register(RegisterRequest req) {

        // Règle métier 1 : email déjà utilisé → erreur claire
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + req.getEmail());
        }

        // Construire l'utilisateur avec mot de passe haché BCrypt
        User user = User.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .email(req.getEmail())
                .motDePasse(passwordEncoder.encode(req.getMotDePasse()))
                .role(req.getRole() != null ? req.getRole() : Role.CLIENT)
                .build();

        // Sauvegarder en base — @PrePersist initialise dateInscription
        User saved = userRepository.save(user);
        log.info("Nouvel utilisateur inscrit : {} ({})", saved.getEmail(), saved.getRole());

        // Générer le token JWT et retourner la réponse complète
        String token = jwtService.generateToken(saved);
        return buildAuthResponse(saved, token);
    }

    // -------------------------------------------------------------------------
    // CONNEXION — vérifie email + mot de passe, génère le token
    // -------------------------------------------------------------------------
    public AuthResponse login(LoginRequest req) {

        // Règle métier 1 : utilisateur inexistant
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + req.getEmail()));

        // Règle métier 2 : mot de passe incorrect
        // passwordEncoder.matches() compare le texte clair avec le hash BCrypt
        if (!passwordEncoder.matches(req.getMotDePasse(), user.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        log.info("Connexion réussie : {}", user.getEmail());

        // Générer le token JWT et retourner la réponse complète
        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    // -------------------------------------------------------------------------
    // PROFIL — retourne les infos de l'utilisateur sans le mot de passe
    // -------------------------------------------------------------------------
    public UserDTO getProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + userId));

        // Mapper vers UserDTO — motDePasse délibérément exclu
        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(user.getRole().name())
                .dateInscription(user.getDateInscription())
                .build();
    }

    // -------------------------------------------------------------------------
    // Méthode privée — construit AuthResponse depuis un User + token
    // -------------------------------------------------------------------------
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .nom(user.getNom())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}