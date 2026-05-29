package com.livraison.livreur.repository;

import com.livraison.livreur.entity.Livreur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivreurRepository extends JpaRepository<Livreur, Long> {

    List<Livreur> findByActifTrue();

    boolean existsByTelephone(String telephone);

    /**
     * Trouve le profil livreur correspondant à un userId du user-service.
     * Permet de faire le lien entre X-User-Id (header Gateway) et livreurId (table livreurs).
     */
    Optional<Livreur> findByUserId(Long userId);
}