package com.livraison.user.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité JPA — mappée sur la table MySQL "users".
 * Chaque instance = une ligne dans la table.
 */
@Entity
@Table(name = "users")          // Nom exact de la table MySQL
@Data                           // Lombok : getters + setters + equals + hashCode + toString
@NoArgsConstructor              // Constructeur vide requis par JPA
@AllArgsConstructor             // Constructeur avec tous les champs
@Builder                        // Pattern builder : User.builder().nom("Jean").build()
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment MySQL
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true, nullable = false) // Email unique en base — contrainte SQL
    private String email;

    @Column(nullable = false)
    private String motDePasse;              // Stocké haché BCrypt — jamais en clair

    @Enumerated(EnumType.STRING)            // Stocke "CLIENT" au lieu de 0, 1, 2
    @Column(nullable = false)
    private Role role;

    @Column(updatable = false)              // Jamais modifié après création
    private LocalDateTime dateInscription;

    /**
     * Appelé automatiquement par JPA avant le premier INSERT.
     * Initialise la date d'inscription et le rôle par défaut si non défini.
     */
    @PrePersist
    public void prePersist() {
        this.dateInscription = LocalDateTime.now();
        if (this.role == null) {
            this.role = Role.CLIENT;
        }
    }
}