package com.livraison.colis.mapper;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.StatutColis;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel pour convertir entre l'entité Colis et les DTOs.
 *
 * Pourquoi centraliser ici ?
 * - Le service et le controller n'ont aucune logique de conversion.
 * - Si la structure change (nouveau champ, renommage), on ne modifie qu'ici.
 *
 * Note : on n'utilise pas MapStruct pour rester explicite et pédagogique.
 * En projet réel complexe, MapStruct génère ce code automatiquement.
 */
@Component
public class ColisMapper {

    /**
     * Convertit un ColisRequestDTO en entité Colis.
     *
     * Champs NON inclus (gérés ailleurs) :
     * - id              → auto-généré par JPA
     * - numeroSuivi     → généré dans le service
     * - statut          → défaut EN_ATTENTE via @Builder.Default
     * - dateCreation    → @CreationTimestamp
     * - dateMiseAJour   → @UpdateTimestamp
     * - createdByUserId → injecté depuis le header X-User-Id dans le service
     *
     * @param dto      les données reçues du client
     * @param userId   l'ID de l'utilisateur extrait du header X-User-Id
     * @return une entité Colis prête à être sauvegardée (sans id ni numeroSuivi)
     */
    public Colis toEntity(ColisRequestDTO dto, Long userId) {
        return Colis.builder()
                .expediteurNom(dto.getExpediteurNom())
                .expediteurAdresse(dto.getExpediteurAdresse())
                .expediteurEmail(dto.getExpediteurEmail())
                .destinataireNom(dto.getDestinataireNom())
                .destinataireAdresse(dto.getDestinataireAdresse())
                .destinataireEmail(dto.getDestinataireEmail())
                .poids(dto.getPoids())
                .description(dto.getDescription())
                .optionService(dto.getOptionService())
                .statut(StatutColis.EN_ATTENTE)     // Toujours EN_ATTENTE à la création
                .createdByUserId(userId)
                .build();
    }

    /**
     * Convertit une entité Colis en ColisResponseDTO.
     *
     * Enrichit la réponse avec :
     * - delaiLivraisonJours : calculé depuis optionService.getDelaiJours()
     *
     * @param colis l'entité récupérée depuis la BDD
     * @return le DTO à renvoyer au client
     */
    public ColisResponseDTO toDTO(Colis colis) {
        return ColisResponseDTO.builder()
                .id(colis.getId())
                .numeroSuivi(colis.getNumeroSuivi())
                .expediteurNom(colis.getExpediteurNom())
                .expediteurAdresse(colis.getExpediteurAdresse())
                .expediteurEmail(colis.getExpediteurEmail())
                .destinataireNom(colis.getDestinataireNom())
                .destinataireAdresse(colis.getDestinataireAdresse())
                .destinataireEmail(colis.getDestinataireEmail())
                .poids(colis.getPoids())
                .description(colis.getDescription())
                .optionService(colis.getOptionService())
                .delaiLivraisonJours(
                    colis.getOptionService() != null
                        ? colis.getOptionService().getDelaiJours()
                        : 0
                )
                .statut(colis.getStatut())
                .dateCreation(colis.getDateCreation())
                .dateMiseAJour(colis.getDateMiseAJour())
                .createdByUserId(colis.getCreatedByUserId())
                .build();
    }
}