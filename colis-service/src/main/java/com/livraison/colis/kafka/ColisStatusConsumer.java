package com.livraison.colis.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.StatutColis;
import com.livraison.colis.repository.ColisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColisStatusConsumer {

    private final ColisRepository colisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
        topics = "colis.status_changed",
        groupId = "colis-service-group"
    )
    @Transactional
    public void onStatusChanged(String message) {
        log.info("[CONSUMER] Message reçu : {}", message);
        try {
            JsonNode event = objectMapper.readTree(message);

            String numeroSuivi    = event.has("numeroSuivi")    ? event.get("numeroSuivi").asText()    : null;
            String nouveauStatutStr = event.has("nouveauStatut") ? event.get("nouveauStatut").asText() : null;
            String ancienStatutStr  = event.has("ancienStatut")  ? event.get("ancienStatut").asText()  : null;
            String livreurNom     = event.has("livreurNom") && !event.get("livreurNom").isNull()
                    ? event.get("livreurNom").asText() : null;
            Long livreurId        = event.has("livreurId") && !event.get("livreurId").isNull()
                    ? event.get("livreurId").asLong() : null;

            if (numeroSuivi == null || nouveauStatutStr == null) {
                log.warn("[CONSUMER] Événement ignoré : champs manquants");
                return;
            }

            StatutColis nouveauStatut;
            try {
                nouveauStatut = StatutColis.valueOf(nouveauStatutStr);
            } catch (IllegalArgumentException e) {
                log.warn("[CONSUMER] Statut inconnu : {}", nouveauStatutStr);
                return;
            }

            Optional<Colis> optColis = colisRepository.findByNumeroSuivi(numeroSuivi);
            if (optColis.isEmpty()) {
                log.warn("[CONSUMER] Colis introuvable : {}", numeroSuivi);
                return;
            }

            Colis colis = optColis.get();

            if (!colis.getStatut().peutTransitionnerVers(nouveauStatut)) {
                log.warn("[CONSUMER] Transition invalide {} → {} pour {}",
                        colis.getStatut(), nouveauStatut, numeroSuivi);
                return;
            }

            colis.setStatut(nouveauStatut);
            if (livreurNom != null) colis.setLivreurNom(livreurNom);
            if (livreurId  != null) colis.setLivreurId(livreurId);
            colisRepository.save(colis);
            log.info("[CONSUMER] ✅ Statut mis à jour {} → {} pour {} livreur={}",
                    ancienStatutStr, nouveauStatut, numeroSuivi, livreurNom);

        } catch (Exception e) {
            log.error("[CONSUMER] Erreur traitement message : {}", e.getMessage());
        }
    }
}