package com.livraison.user.repository;

import com.livraison.user.model.Role;
import com.livraison.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès base de données pour l'entité User.
 * Spring génère automatiquement le SQL à partir des noms de méthodes.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM users WHERE email = ? LIMIT 1
    Optional<User> findByEmail(String email);

    // SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);

    // SELECT * FROM users WHERE role = ?
    List<User> findByRole(Role role);
}