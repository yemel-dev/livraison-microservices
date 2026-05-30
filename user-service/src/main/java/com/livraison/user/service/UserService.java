package com.livraison.user.service;

import com.livraison.user.dto.*;
import com.livraison.user.kafka.UserCreatedEvent;
import com.livraison.user.kafka.UserKafkaProducer;
import com.livraison.user.model.Role;
import com.livraison.user.model.User;
import com.livraison.user.repository.UserRepository;
import com.livraison.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository    userRepository;
    private final JwtService        jwtService;
    private final PasswordEncoder   passwordEncoder;
    private final UserKafkaProducer kafkaProducer;

    public AuthResponse register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + req.getEmail());
        }

        User user = User.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .email(req.getEmail())
                .motDePasse(passwordEncoder.encode(req.getMotDePasse()))
                .role(req.getRole() != null ? req.getRole() : Role.CLIENT)
                .build();

        User saved = userRepository.save(user);
        log.info("Nouvel utilisateur inscrit : {} ({})", saved.getEmail(), saved.getRole());

        // Publier sur Kafka uniquement si le rôle est LIVREUR
        if (saved.getRole() == Role.LIVREUR) {
            kafkaProducer.publierUserCreated(
                UserCreatedEvent.builder()
                    .userId(saved.getId())
                    .nom(saved.getNom())
                    .prenom(saved.getPrenom())
                    .email(saved.getEmail())
                    .role(saved.getRole().name())
                    .build()
            );
        }

        String token = jwtService.generateToken(saved);
        return buildAuthResponse(saved, token);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + req.getEmail()));

        if (!passwordEncoder.matches(req.getMotDePasse(), user.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        log.info("Connexion réussie : {}", user.getEmail());
        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    public UserDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + userId));

        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(user.getRole().name())
                .dateInscription(user.getDateInscription())
                .build();
    }

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