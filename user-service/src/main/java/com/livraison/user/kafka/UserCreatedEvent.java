package com.livraison.user.kafka;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    private Long   userId;
    private String nom;
    private String prenom;
    private String email;
    private String role;
}