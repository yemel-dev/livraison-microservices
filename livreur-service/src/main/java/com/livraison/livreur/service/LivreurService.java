package com.livraison.livreur.service;

import com.livraison.livreur.dto.CreateLivreurRequest;
import com.livraison.livreur.dto.LivreurResponse;
import com.livraison.livreur.dto.UpdateLivreurRequest;
import com.livraison.livreur.entity.Livreur;
import com.livraison.livreur.exception.ResourceNotFoundException;
import com.livraison.livreur.repository.LivreurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LivreurService {

    private final LivreurRepository livreurRepository;

    // Créer un livreur
    public LivreurResponse creerLivreur(CreateLivreurRequest request) {
        if (livreurRepository.existsByTelephone(request.getTelephone())) {
            throw new IllegalStateException(
                    "Un livreur avec le téléphone " + request.getTelephone() + " existe déjà");
        }

        Livreur livreur = Livreur.builder()
                .userId(request.getUserId())   // ← lien avec le user-service
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .telephone(request.getTelephone())
                .vehicule(request.getVehicule())
                .actif(true)
                .build();

        Livreur saved = livreurRepository.save(livreur);
        log.info("[LIVREUR] Nouveau livreur créé : id={}, userId={}, nom={}",
                saved.getId(), saved.getUserId(), saved.getNom());
        return toResponse(saved);
    }

    // Lister tous les livreurs actifs
    @Transactional(readOnly = true)
    public List<LivreurResponse> listerLivreursActifs() {
        return livreurRepository.findByActifTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Lister tous les livreurs (ADMIN)
    @Transactional(readOnly = true)
    public List<LivreurResponse> listerTousLivreurs() {
        return livreurRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Obtenir un livreur par ID
    @Transactional(readOnly = true)
    public LivreurResponse getLivreurById(Long id) {
        return toResponse(findLivreurOuErreur(id));
    }

    // Mettre à jour un livreur
    public LivreurResponse mettreAJourLivreur(Long id, UpdateLivreurRequest request) {
        Livreur livreur = findLivreurOuErreur(id);

        if (request.getNom() != null)       livreur.setNom(request.getNom());
        if (request.getPrenom() != null)    livreur.setPrenom(request.getPrenom());
        if (request.getVehicule() != null)  livreur.setVehicule(request.getVehicule());

        if (request.getTelephone() != null
                && !request.getTelephone().equals(livreur.getTelephone())) {
            if (livreurRepository.existsByTelephone(request.getTelephone())) {
                throw new IllegalStateException("Ce numéro de téléphone est déjà utilisé");
            }
            livreur.setTelephone(request.getTelephone());
        }

        return toResponse(livreurRepository.save(livreur));
    }

    // Activer / Désactiver un livreur
    public LivreurResponse toggleActif(Long id) {
        Livreur livreur = findLivreurOuErreur(id);
        livreur.setActif(!livreur.isActif());
        log.info("[LIVREUR] Statut livreur id={} → actif={}", id, livreur.isActif());
        return toResponse(livreurRepository.save(livreur));
    }

    // Supprimer un livreur (soft delete)
    public void supprimerLivreur(Long id) {
        Livreur livreur = findLivreurOuErreur(id);
        livreur.setActif(false);
        livreurRepository.save(livreur);
        log.info("[LIVREUR] Livreur id={} désactivé (soft delete)", id);
    }

    // Helpers
    public Livreur findLivreurOuErreur(Long id) {
        return livreurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Livreur non trouvé avec l'id : " + id));
    }

    private LivreurResponse toResponse(Livreur livreur) {
        return LivreurResponse.builder()
                .id(livreur.getId())
                .userId(livreur.getUserId())
                .nom(livreur.getNom())
                .prenom(livreur.getPrenom())
                .telephone(livreur.getTelephone())
                .vehicule(livreur.getVehicule())
                .actif(livreur.isActif())
                .build();
    }
}