package com.livraison.user.dto;

import lombok.*;
import java.time.LocalDateTime;

/** Profil utilisateur sans mot de passe — retourné par GET /api/auth/users/me. */
@Data
@Builder
public class UserDTO {

    private Long          id;
    private String        nom;
    private String        prenom;
    private String        email;
    private String        role;
    private LocalDateTime dateInscription;
}