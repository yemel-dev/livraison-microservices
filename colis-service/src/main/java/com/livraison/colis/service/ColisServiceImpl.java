package com.livraison.colis.service;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.StatutColis;
import com.livraison.colis.exception.ColisNotFoundException;
import com.livraison.colis.exception.InvalidStatutTransitionException;
import com.livraison.colis.kafka.ColisKafkaProducer;
import com.livraison.colis.kafka.event.ColisCreatedEvent;
import com.livraison.colis.kafka.event.ColisStatusChangedEvent;
import com.livraison.colis.mapper.ColisMapper;
import com.livraison.colis.repository.ColisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Implémentation finale du service métier — avec intégration Kafka.
 *
 * Ordre garanti dans creerColis() et updateStatut() :
 *   1. Sauvegarde BDD (transaction committée)
 *   2. Publication Kafka (seulement si BDD OK)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ColisServiceImpl implements ColisService {

    private final ColisRepository    colisRepository;
    private final ColisMapper        colisMapper;
    private final ColisKafkaProducer kafkaProducer;

    private static final String TRACKING_PREFIX = "COL";
    private static final String ROLE_ADMIN      = "ADMIN";
    private static final String ROLE_LIVREUR    = "LIVREUR";
    private static final int    MAX_RETRY_SUIVI = 5;

    // ─── Création ────────────────────────────────────────────────────────────

    @Override
    public ColisResponseDTO creerColis(ColisRequestDTO dto, Long userId) {
        log.info("Création d'un colis pour l'utilisateur {}", userId);

        Colis colis = colisMapper.toEntity(dto, userId);
        colis.setNumeroSuivi(genererNumeroSuiviUnique());

        // 1. Commit BDD d'abord
        Colis saved = colisRepository.save(colis);
        log.info("Colis créé : id={}, numeroSuivi={}", saved.getId(), saved.getNumeroSuivi());

        // 2. Publication Kafka après commit
        kafkaProducer.publishColisCreated(ColisCreatedEvent.builder()
                .colisId(saved.getId())
                .numeroSuivi(saved.getNumeroSuivi())
                .destinataireEmail(saved.getDestinataireEmail())
                .destinataireNom(saved.getDestinataireNom())
                .expediteurNom(saved.getExpediteurNom())
                .expediteurEmail(saved.getExpediteurEmail())
                .optionService(saved.getOptionService())
                .delaiLivraisonJours(saved.getOptionService().getDelaiJours())
                .dateCreation(saved.getDateCreation())
                .createdByUserId(saved.getCreatedByUserId())
                .build());

        return colisMapper.toDTO(saved);
    }

    // ─── Lecture ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ColisResponseDTO getColisById(Long id, Long userId, String role) {
        log.debug("Récupération colis id={} par userId={}, role={}", id, userId, role);
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));
        verifierAccesLecture(colis, userId, role);
        return colisMapper.toDTO(colis);
    }

    @Override
    @Transactional(readOnly = true)
    public ColisResponseDTO getColisByNumeroSuivi(String numeroSuivi) {
        log.debug("Récupération colis numeroSuivi={}", numeroSuivi);
        Colis colis = colisRepository.findByNumeroSuivi(numeroSuivi)
                .orElseThrow(() -> new ColisNotFoundException(numeroSuivi));
        return colisMapper.toDTO(colis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColisResponseDTO> getAllColis(Long userId, String role) {
        log.debug("Liste colis pour userId={}, role={}", userId, role);
        List<Colis> colis = ROLE_ADMIN.equals(role)
                ? colisRepository.findAll()
                : colisRepository.findByCreatedByUserId(userId);
        return colis.stream().map(colisMapper::toDTO).collect(Collectors.toList());
    }

    // ─── Mise à jour ─────────────────────────────────────────────────────────

    @Override
    public ColisResponseDTO updateColis(Long id, ColisRequestDTO dto, Long userId, String role) {
        log.info("Mise à jour colis id={} par userId={}", id, userId);
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));
        verifierAccesEcriture(colis, userId, role);
        if (colis.getStatut() != StatutColis.EN_ATTENTE) {
            throw new IllegalStateException(
                "Modification impossible : statut actuel = " + colis.getStatut() +
                ". Seul un colis EN_ATTENTE peut être modifié.");
        }
        colis.setExpediteurNom(dto.getExpediteurNom());
        colis.setExpediteurAdresse(dto.getExpediteurAdresse());
        colis.setExpediteurEmail(dto.getExpediteurEmail());
        colis.setDestinataireNom(dto.getDestinataireNom());
        colis.setDestinataireAdresse(dto.getDestinataireAdresse());
        colis.setDestinataireEmail(dto.getDestinataireEmail());
        colis.setPoids(dto.getPoids());
        colis.setDescription(dto.getDescription());
        colis.setOptionService(dto.getOptionService());
        return colisMapper.toDTO(colisRepository.save(colis));
    }

    @Override
    public ColisResponseDTO updateStatut(Long id, StatutColis nouveauStatut, Long userId, String role) {
        log.info("Changement statut colis id={} → {} par userId={}", id, nouveauStatut, userId);

        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));

        if (!ROLE_ADMIN.equals(role) && !ROLE_LIVREUR.equals(role)) {
            throw new AccessDeniedException("Seuls les livreurs et administrateurs peuvent changer le statut.");
        }

        StatutColis ancienStatut = colis.getStatut();
        if (!ancienStatut.peutTransitionnerVers(nouveauStatut)) {
            throw new InvalidStatutTransitionException(ancienStatut, nouveauStatut);
        }

        // 1. Commit BDD
        colis.changerStatut(nouveauStatut);
        Colis updated = colisRepository.save(colis);
        log.info("Statut colis id={} : {} → {}", id, ancienStatut, nouveauStatut);

        // 2. Publication Kafka après commit
        kafkaProducer.publishStatusChanged(ColisStatusChangedEvent.builder()
                .colisId(updated.getId())
                .numeroSuivi(updated.getNumeroSuivi())
                .ancienStatut(ancienStatut)
                .nouveauStatut(nouveauStatut)
                .destinataireEmail(updated.getDestinataireEmail())
                .destinataireNom(updated.getDestinataireNom())
                .dateMiseAJour(updated.getDateMiseAJour())
                .modifiePar(userId)
                .build());

        return colisMapper.toDTO(updated);
    }

    // ─── Suppression ─────────────────────────────────────────────────────────

    @Override
    public void deleteColis(Long id, Long userId, String role) {
        log.info("Suppression colis id={} par userId={}", id, userId);
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));
        if (!ROLE_ADMIN.equals(role)) {
            throw new AccessDeniedException("Seuls les administrateurs peuvent supprimer un colis.");
        }
        if (colis.getStatut() != StatutColis.EN_ATTENTE) {
            throw new IllegalStateException(
                "Suppression impossible : statut actuel = " + colis.getStatut() +
                ". Seul un colis EN_ATTENTE peut être supprimé.");
        }
        colisRepository.delete(colis);
        log.info("Colis id={} supprimé", id);
    }

    // ─── Méthodes privées ────────────────────────────────────────────────────

    private String genererNumeroSuiviUnique() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < MAX_RETRY_SUIVI; i++) {
            String candidat = TRACKING_PREFIX + "-" + date + "-" + genererSuffixeAleatoire(5);
            if (!colisRepository.existsByNumeroSuivi(candidat)) {
                return candidat;
            }
            log.warn("Collision numéro de suivi (tentative {}/{})", i + 1, MAX_RETRY_SUIVI);
        }
        throw new IllegalStateException("Impossible de générer un numéro de suivi unique après " + MAX_RETRY_SUIVI + " tentatives.");
    }

    private String genererSuffixeAleatoire(int longueur) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void verifierAccesLecture(Colis colis, Long userId, String role) {
        if (!ROLE_ADMIN.equals(role) && !colis.getCreatedByUserId().equals(userId)) {
            log.warn("Accès refusé : userId={} tente d'accéder au colis id={} de userId={}",
                    userId, colis.getId(), colis.getCreatedByUserId());
            throw new AccessDeniedException("Vous n'avez pas accès à ce colis.");
        }
    }

    private void verifierAccesEcriture(Colis colis, Long userId, String role) {
        if (!ROLE_ADMIN.equals(role) && !colis.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier ce colis.");
        }
    }
}