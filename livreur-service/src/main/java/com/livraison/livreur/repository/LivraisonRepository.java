package com.livraison.livreur.repository;

import com.livraison.livreur.entity.Livraison;
import com.livraison.livreur.enums.StatutLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long>{

    // Tournée du jour : livraisons assignées aujourd'hui à un livreur
    @Query("SELECT l FROM Livraison l WHERE l.livreur.id = :livreurId " +
            "AND l.dateAssignation >= :debutJournee " +
            "AND l.dateAssignation < :finJournee " +
            "ORDER BY l.dateAssignation ASC")
    List<Livraison> findTourneeJour(
            @Param("livreurId") Long livreurId,
            @Param("debutJournee") LocalDateTime debutJournee,
            @Param("finJournee") LocalDateTime finJournee
    );

    // Toutes les livraisons d'un livreur
    List<Livraison> findByLivreurIdOrderByDateAssignationDesc(Long livreurId);

    // Livraisons par statut pour un livreur
    List<Livraison> findByLivreurIdAndStatut(Long livreurId, StatutLivraison statut);

    // Chercher par numéro de suivi
    Optional<Livraison> findByNumeroSuivi(String numeroSuivi);

    // Vérifier si un livreur est bien assigné à une livraison (Zero Trust)
    boolean existsByIdAndLivreurId(Long livraisonId, Long livreurId);
}
