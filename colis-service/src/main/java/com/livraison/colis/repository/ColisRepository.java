package com.livraison.colis.repository;

import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.StatutColis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Colis.
 *
 * JpaRepository<Colis, Long> fournit automatiquement :
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), ...
 *
 * Les méthodes ci-dessous sont des "derived queries" :
 * Spring génère le SQL automatiquement à partir du nom de la méthode.
 */
@Repository
public interface ColisRepository extends JpaRepository<Colis, Long> {

    /**
     * Recherche un colis par son numéro de suivi unique.
     * Utilisé pour :
     *   - l'endpoint GET /api/colis/suivi/{numeroSuivi}
     *   - vérifier l'unicité lors de la génération du numéro de suivi
     */
    Optional<Colis> findByNumeroSuivi(String numeroSuivi);

    /**
     * Récupère tous les colis créés par un utilisateur donné.
     * Utilisé pour le filtrage CLIENT : un client ne voit que ses colis.
     */
    List<Colis> findByCreatedByUserId(Long userId);

    /**
     * Récupère tous les colis ayant un statut donné.
     * Utile pour les tableaux de bord admin ou les livreurs.
     */
    List<Colis> findByStatut(StatutColis statut);

    /**
     * Récupère les colis d'un utilisateur filtrés par statut.
     * Combinaison des deux filtres précédents.
     */
    List<Colis> findByCreatedByUserIdAndStatut(Long userId, StatutColis statut);

    /**
     * Vérifie si un numéro de suivi existe déjà en BDD.
     * Utilisé lors de la génération pour garantir l'unicité.
     * Plus efficace que findByNumeroSuivi() car ne charge pas l'entité complète.
     */
    boolean existsByNumeroSuivi(String numeroSuivi);
}