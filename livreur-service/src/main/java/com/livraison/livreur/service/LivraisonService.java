package com.livraison.livreur.service;

import com.livraison.livreur.dto.AssignerLivraisonRequest;
import com.livraison.livreur.dto.EchecRequest;
import com.livraison.livreur.dto.LivraisonResponse;
import com.livraison.livreur.entity.Livraison;
import com.livraison.livreur.entity.Livreur;
import com.livraison.livreur.enums.StatutColis;
import com.livraison.livreur.enums.StatutLivraison;
import com.livraison.livreur.exception.AccessDeniedException;
import com.livraison.livreur.exception.ResourceNotFoundException;
import com.livraison.livreur.kafka.ColisStatusChangedEvent;
import com.livraison.livreur.kafka.KafkaProducerService;
import com.livraison.livreur.kafka.LivraisonDoneEvent;
import com.livraison.livreur.repository.LivraisonRepository;
import com.livraison.livreur.repository.LivreurRepository;
import com.livraison.livreur.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LivraisonService {

    private final LivraisonRepository livraisonRepository;
    private final LivreurService      livreurService;
    private final LivreurRepository   livreurRepository;
    private final KafkaProducerService kafkaProducerService;

    // -------------------------------------------------------------------------
    // Assigner un colis à un livreur (ADMIN)
    // -------------------------------------------------------------------------
    public LivraisonResponse assignerColis(AssignerLivraisonRequest request) {
        Livreur livreur = livreurService.findLivreurOuErreur(request.getLivreurId());

        if (!livreur.isActif()) {
            throw new IllegalStateException(
                    "Le livreur id=" + request.getLivreurId() + " est désactivé");
        }

        Livraison livraison = Livraison.builder()
                .numeroSuivi(request.getNumeroSuivi())
                .livreur(livreur)
                .statut(StatutLivraison.ASSIGNEE)
                .dateAssignation(LocalDateTime.now())
                .build();

        Livraison saved = livraisonRepository.save(livraison);
        log.info("[LIVRAISON] Colis {} assigné au livreur id={}", request.getNumeroSuivi(), livreur.getId());

        ColisStatusChangedEvent event = ColisStatusChangedEvent.builder()
        .numeroSuivi(request.getNumeroSuivi())
        .ancienStatut(StatutColis.EN_ATTENTE)
        .nouveauStatut(StatutColis.ENLEVE)
        .dateChangement(LocalDateTime.now())
        .livreurId(livreur.getId())
        .livreurNom(livreur.getNom() + " " + livreur.getPrenom())
        .build();
kafkaProducerService.publierColisStatusChanged(event);
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Tournée du jour d'un livreur
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<LivraisonResponse> getTourneeJour(Long livreurId) {
        verifierAccesTournee(livreurId);

        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin   = debut.plusDays(1);

        return livraisonRepository.findTourneeJour(livreurId, debut, fin)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Toutes les livraisons d'un livreur
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<LivraisonResponse> getLivraisonsLivreur(Long livreurId) {
        verifierAccesTournee(livreurId);
        return livraisonRepository.findByLivreurIdOrderByDateAssignationDesc(livreurId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Scan — Prise en charge
    // -------------------------------------------------------------------------
    public LivraisonResponse scannerPriseEnCharge(Long livraisonId, Long livreurId) {
        Livraison livraison = findEtVerifierPropriete(livraisonId, livreurId);

        if (livraison.getStatut() != StatutLivraison.ASSIGNEE) {
            throw new IllegalStateException(
                    "La livraison doit être en statut ASSIGNEE pour être prise en charge. " +
                            "Statut actuel : " + livraison.getStatut());
        }

        livraison.setStatut(StatutLivraison.ENLEVEE);
        Livraison saved = livraisonRepository.save(livraison);

        log.info("[LIVRAISON] Colis {} pris en charge par livreur id={}", livraison.getNumeroSuivi(), livreurId);
        publierChangementStatut(livraison.getNumeroSuivi(), StatutColis.EN_ATTENTE, StatutColis.ENLEVE);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Mise à jour en transit
    // -------------------------------------------------------------------------
    public LivraisonResponse mettreEnTransit(Long livraisonId, Long livreurId) {
        Livraison livraison = findEtVerifierPropriete(livraisonId, livreurId);

        if (livraison.getStatut() != StatutLivraison.ENLEVEE) {
            throw new IllegalStateException(
                    "La livraison doit être en statut ENLEVEE pour passer en transit. " +
                            "Statut actuel : " + livraison.getStatut());
        }

        livraison.setStatut(StatutLivraison.EN_COURS);
        Livraison saved = livraisonRepository.save(livraison);

        log.info("[LIVRAISON] Colis {} en transit — livreur id={}", livraison.getNumeroSuivi(), livreurId);
        publierChangementStatut(livraison.getNumeroSuivi(), StatutColis.ENLEVE, StatutColis.EN_TRANSIT);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Confirmation livraison réussie
    // -------------------------------------------------------------------------
    public LivraisonResponse confirmerLivraison(Long livraisonId, Long livreurId) {
        Livraison livraison = findEtVerifierPropriete(livraisonId, livreurId);

        if (livraison.isTermine()) {
            throw new IllegalStateException("Cette livraison est déjà terminée");
        }

        livraison.confirmerLivraison();
        Livraison saved = livraisonRepository.save(livraison);

        log.info("[LIVRAISON] Livraison {} confirmée par livreur id={}", livraison.getNumeroSuivi(), livreurId);

        LivraisonDoneEvent doneEvent = LivraisonDoneEvent.builder()
                .numeroSuivi(livraison.getNumeroSuivi())
                .livreurId(livreurId)
                .livreurNom(livraison.getLivreur().getNom() + " " + livraison.getLivreur().getPrenom())
                .dateLivraison(saved.getDateLivraison())
                .build();
        kafkaProducerService.publierLivraisonDone(doneEvent);

        publierChangementStatut(livraison.getNumeroSuivi(), StatutColis.EN_LIVRAISON, StatutColis.LIVRE);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Enregistrement d'un échec
    // -------------------------------------------------------------------------
    public LivraisonResponse enregistrerEchec(Long livraisonId, Long livreurId, EchecRequest request) {
        Livraison livraison = findEtVerifierPropriete(livraisonId, livreurId);

        if (livraison.isTermine()) {
            throw new IllegalStateException("Cette livraison est déjà terminée");
        }

        livraison.enregistrerEchec(request.getMotifEchec());
        Livraison saved = livraisonRepository.save(livraison);

        log.warn("[LIVRAISON] Échec de livraison {} — motif : {}", livraison.getNumeroSuivi(), request.getMotifEchec());
        publierChangementStatut(livraison.getNumeroSuivi(), StatutColis.EN_LIVRAISON, StatutColis.ECHEC_LIVRAISON);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Détail d'une livraison
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public LivraisonResponse getLivraison(Long livraisonId) {
        Livraison livraison = livraisonRepository.findById(livraisonId)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison non trouvée : " + livraisonId));
        return toResponse(livraison);
    }

    // -------------------------------------------------------------------------
    // Zero Trust : vérification propriété de la livraison
    // Compare le userId du token avec le userId stocké dans le profil livreur
    // -------------------------------------------------------------------------
    private Livraison findEtVerifierPropriete(Long livraisonId, Long livreurId) {
        Livraison livraison = livraisonRepository.findById(livraisonId)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison non trouvée : " + livraisonId));

        if (SecurityContext.isAdmin()) return livraison;

        // Récupérer le userId du token JWT (injecté par le Gateway)
        Long currentUserId = SecurityContext.getCurrentUserId();

        // Récupérer le profil livreur et vérifier son userId
        Livreur livreur = livraison.getLivreur();
        if (currentUserId == null || !currentUserId.equals(livreur.getUserId())) {
            log.warn("[ZERO TRUST] Accès non autorisé — currentUserId={} vs livreur.userId={}",
                    currentUserId, livreur.getUserId());
            throw new AccessDeniedException(
                    "Accès refusé : vous n'êtes pas assigné à cette livraison");
        }
        return livraison;
    }

    // -------------------------------------------------------------------------
    // Zero Trust : vérification accès tournée
    // -------------------------------------------------------------------------
    private void verifierAccesTournee(Long livreurId) {
        if (SecurityContext.isAdmin()) return;

        Long currentUserId = SecurityContext.getCurrentUserId();

        // Trouver le profil livreur correspondant au userId du token
        Livreur livreur = livreurRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Aucun profil livreur trouvé pour cet utilisateur"));

        if (!livreur.getId().equals(livreurId)) {
            throw new AccessDeniedException(
                    "Accès refusé : vous ne pouvez consulter que votre propre tournée");
        }
    }

    // -------------------------------------------------------------------------
    // Kafka helper
    // -------------------------------------------------------------------------
    private void publierChangementStatut(String numeroSuivi, StatutColis ancien, StatutColis nouveau) {
        ColisStatusChangedEvent event = ColisStatusChangedEvent.builder()
                .numeroSuivi(numeroSuivi)
                .ancienStatut(ancien)
                .nouveauStatut(nouveau)
                .dateChangement(LocalDateTime.now())
                .build();
        kafkaProducerService.publierColisStatusChanged(event);
    }

    // -------------------------------------------------------------------------
    // Mapper entity → DTO
    // -------------------------------------------------------------------------
    private LivraisonResponse toResponse(Livraison l) {
        return LivraisonResponse.builder()
                .id(l.getId())
                .numeroSuivi(l.getNumeroSuivi())
                .livreurId(l.getLivreur().getId())
                .livreurNom(l.getLivreur().getNom() + " " + l.getLivreur().getPrenom())
                .statut(l.getStatut())
                .dateAssignation(l.getDateAssignation())
                .dateLivraison(l.getDateLivraison())
                .motifEchec(l.getMotifEchec())
                .build();
    }
}