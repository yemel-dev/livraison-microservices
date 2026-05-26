package com.livraison.colis.service;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.enums.StatutColis;

import java.util.List;

/**
 * Interface du service métier pour les colis.
 *
 * Pourquoi une interface ?
 * - Facilite les tests unitaires : on peut mocker ColisService sans charger Spring.
 * - Respecte le principe d'inversion de dépendance (SOLID).
 * - Le controller dépend de l'interface, pas de l'implémentation.
 */
public interface ColisService {

    /**
     * Crée un nouveau colis.
     * Génère le numéro de suivi, fixe le statut EN_ATTENTE, publie l'event Kafka.
     *
     * @param dto    données de création fournies par le client
     * @param userId ID de l'utilisateur créateur (extrait du header X-User-Id)
     * @return le colis créé avec son numéro de suivi et son ID
     */
    ColisResponseDTO creerColis(ColisRequestDTO dto, Long userId);

    /**
     * Récupère un colis par son ID.
     * Vérifie que le CLIENT ne peut accéder qu'à SES colis (isolation).
     *
     * @param id     ID du colis
     * @param userId ID de l'utilisateur demandeur
     * @param role   rôle de l'utilisateur (ROLE_CLIENT, ROLE_ADMIN, etc.)
     * @return le colis correspondant
     */
    ColisResponseDTO getColisById(Long id, Long userId, String role);

    /**
     * Récupère un colis par son numéro de suivi.
     * Accessible à tous (endpoint de suivi public).
     *
     * @param numeroSuivi le numéro de suivi (ex: COL-20240315-A3F7K)
     * @return le colis correspondant
     */
    ColisResponseDTO getColisByNumeroSuivi(String numeroSuivi);

    /**
     * Liste les colis selon le rôle :
     * - CLIENT → uniquement ses colis (filtrés par userId)
     * - ADMIN  → tous les colis
     *
     * @param userId ID de l'utilisateur demandeur
     * @param role   rôle de l'utilisateur
     * @return liste des colis accessibles
     */
    List<ColisResponseDTO> getAllColis(Long userId, String role);

    /**
     * Met à jour les informations d'un colis.
     * Uniquement si le colis est en statut EN_ATTENTE.
     * Seul le propriétaire ou un ADMIN peut modifier.
     *
     * @param id     ID du colis à modifier
     * @param dto    nouvelles données
     * @param userId ID de l'utilisateur demandeur
     * @param role   rôle de l'utilisateur
     * @return le colis mis à jour
     */
    ColisResponseDTO updateColis(Long id, ColisRequestDTO dto, Long userId, String role);

    /**
     * Change le statut d'un colis.
     * Valide la transition selon le cycle de vie défini dans StatutColis.
     * Publie un event Kafka colis.status_changed.
     *
     * @param id           ID du colis
     * @param nouveauStatut statut cible
     * @param userId       ID de l'utilisateur demandeur
     * @param role         rôle de l'utilisateur
     * @return le colis avec son nouveau statut
     */
    ColisResponseDTO updateStatut(Long id, StatutColis nouveauStatut, Long userId, String role);

    /**
     * Supprime un colis.
     * Uniquement si le colis est en statut EN_ATTENTE.
     * Réservé aux ADMIN.
     *
     * @param id     ID du colis à supprimer
     * @param userId ID de l'utilisateur demandeur
     * @param role   rôle de l'utilisateur
     */
    void deleteColis(Long id, Long userId, String role);
}