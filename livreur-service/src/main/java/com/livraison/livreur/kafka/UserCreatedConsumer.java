package com.livraison.livreur.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.livreur.entity.Livreur;
import com.livraison.livreur.repository.LivreurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreatedConsumer {

    private final LivreurRepository livreurRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user.created", groupId = "livreur-service-group")
    @Transactional
    public void onUserCreated(String message) {
        log.info("[CONSUMER] user.created reçu : {}", message);
        try {
            JsonNode event = objectMapper.readTree(message);
            String role = event.has("role") ? event.get("role").asText() : null;

            // On ne traite que les nouveaux LIVREURS
            if (!"LIVREUR".equals(role)) return;

            Long   userId = event.get("userId").asLong();
            String nom    = event.get("nom").asText();
            String prenom = event.get("prenom").asText();

            // Éviter les doublons si le message est rejoué
            if (livreurRepository.existsByUserId(userId)) {
                log.info("[CONSUMER] Profil livreur déjà existant pour userId={}", userId);
                return;
            }

            Livreur livreur = Livreur.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .userId(userId)
                    .actif(true)
                    .vehicule("Non défini")
                    .telephone("0000000000")
                    .build();

            livreurRepository.save(livreur);
            log.info("[CONSUMER] ✅ Profil livreur créé automatiquement userId={} → {}", userId, nom);

        } catch (Exception e) {
            log.error("[CONSUMER] ❌ Erreur traitement user.created : {}", e.getMessage(), e);
        }
    }
}