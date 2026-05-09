package com.livraison.livreur.repository;

import com.livraison.livreur.entity.Livreur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivreurRepository extends JpaRepository<Livreur, Long> {

    List<Livreur> findByActifTrue();

    Optional<Livreur> findByTelephone(String telephone);

    boolean existsByTelephone(String telephone);
}
