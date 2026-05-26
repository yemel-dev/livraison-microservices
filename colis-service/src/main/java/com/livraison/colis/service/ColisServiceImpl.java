package com.livraison.colis.service;

import com.livraison.colis.dto.ColisRequestDTO;
import com.livraison.colis.dto.ColisResponseDTO;
import com.livraison.colis.entity.Colis;
import com.livraison.colis.enums.StatutColis;
import com.livraison.colis.exception.ColisNotFoundException;
import com.livraison.colis.exception.InvalidStatutTransitionException;
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
 * Implémentation du service métier pour les colis.
 *
 * Responsabilités :
 * 1. Validation métier (transitions de statut, droits d'accès, règles de suppression)
 * 2. Génération du numéro de suivi unique
 * 3. Persistance via ColisRepository
 * 4. Publication des événements Kafka (ajoutée à l'étape 17)
 *
 * Note : les appels Kafka sont commentés pour l'instant (étape 17).
 * Ils seront décommentés quand ColisKafkaProducer sera créé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ColisServiceImpl implements ColisService {

    private final ColisRepository colisRepository;
    private final ColisMapper colisMapper;
    // @Autowired sera ajouté à l'étape 17 :
    // private final ColisKafkaProducer kafkaProducer;

    // ─── Constantes ──────────────────────────────────────────────────────────

    private static final String TRACKING_PREFIX = "COL";
    private static final String ROLE_ADMIN      = "ROLE_ADMIN";
    private static final String ROLE_LIVREUR    = "ROLE_LIVREUR";
    private static final int    MAX_RETRY_SUIVI = 5;  // Nb max de tentatives de génération

    // ─── Création ────────────────────────────────────────────────────────────

    @Override
    public ColisResponseDTO creerColis(ColisRequestDTO dto, Long userId) {
        log.info("Création d'un colis pour l'utilisateur {}", userId);

        // 1. Convertir le DTO en entité
        Colis colis = colisMapper.toEntity(dto, userId);

        // 2. Générer un numéro de suivi unique
        String numeroSuivi = genererNumeroSuiviUnique();
        colis.setNumeroSuivi(numeroSuivi);

        // 3. Sauvegarder en BDD (transaction committée avant Kafka)
        Colis saved = colisRepository.save(colis);
        log.info("Colis créé avec succès : id={}, numeroSuivi={}", saved.getId(), saved.getNumeroSuivi());

        // 4. TODO étape 17 — Publier l'événement Kafka après commit BDD
        // kafkaProducer.publishColisCreated(new ColisCreatedEvent(...));

        return colisMapper.toDTO(saved);
    }

    // ─── Lecture ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ColisResponseDTO getColisById(Long id, Long userId, String role) {
        log.debug("Récupération du colis id={} par userId={}, role={}", id, userId, role);

        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));

        // Vérification d'ownership : un CLIENT ne peut voir que SES colis
        verifierAccesLecture(colis, userId, role);

        return colisMapper.toDTO(colis);
    }

    @Override
    @Transactional(readOnly = true)
    public ColisResponseDTO getColisByNumeroSuivi(String numeroSuivi) {
        log.debug("Récupération du colis numeroSuivi={}", numeroSuivi);

        Colis colis = colisRepository.findByNumeroSuivi(numeroSuivi)
                .orElseThrow(() -> new ColisNotFoundException(numeroSuivi));

        return colisMapper.toDTO(colis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColisResponseDTO> getAllColis(Long userId, String role) {
        log.debug("Liste des colis pour userId={}, role={}", userId, role);

        List<Colis> colis;

        if (ROLE_ADMIN.equals(role)) {
            // ADMIN voit tous les colis
            colis = colisRepository.findAll();
        } else {
            // CLIENT voit uniquement ses colis
            colis = colisRepository.findByCreatedByUserId(userId);
        }

        return colis.stream()
                .map(colisMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ─── Mise à jour ─────────────────────────────────────────────────────────

    @Override
    public ColisResponseDTO updateColis(Long id, ColisRequestDTO dto, Long userId, String role) {
        log.info("Mise à jour du colis id={} par userId={}", id, userId);

        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));

        // Vérification ownership
        verifierAccesEcriture(colis, userId, role);

        // Un colis ne peut être modifié que s'il est EN_ATTENTE
        if (colis.getStatut() != StatutColis.EN_ATTENTE) {
            throw new IllegalStateException(
                "Un colis ne peut être modifié qu'en statut EN_ATTENTE. Statut actuel : " + colis.getStatut()
            );
        }

        // Mise à jour des champs (le numéro de suivi et l'ID ne changent pas)
        colis.setExpediteurNom(dto.getExpediteurNom());
        colis.setExpediteurAdresse(dto.getExpediteurAdresse());
        colis.setExpediteurEmail(dto.getExpediteurEmail());
        colis.setDestinataireNom(dto.getDestinataireNom());
        colis.setDestinataireAdresse(dto.getDestinataireAdresse());
        colis.setDestinataireEmail(dto.getDestinataireEmail());
        colis.setPoids(dto.getPoids());
        colis.setDescription(dto.getDescription());
        colis.setOptionService(dto.getOptionService());

        Colis updated = colisRepository.save(colis);
        log.info("Colis id={} mis à jour avec succès", id);

        return colisMapper.toDTO(updated);
    }

    @Override
    public ColisResponseDTO updateStatut(Long id, StatutColis nouveauStatut, Long userId, String role) {
        log.info("Changement de statut du colis id={} : → {} par userId={}", id, nouveauStatut, userId);

        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));

        // Seuls LIVREUR et ADMIN peuvent changer le statut
        if (!ROLE_ADMIN.equals(role) && !ROLE_LIVREUR.equals(role)) {
            throw new AccessDeniedException("Seuls les livreurs et administrateurs peuvent changer le statut d'un colis.");
        }

        StatutColis statutActuel = colis.getStatut();

        // Vérifier que la transition est valide selon le cycle de vie
        if (!statutActuel.peutTransitionnerVers(nouveauStatut)) {
            throw new InvalidStatutTransitionException(statutActuel, nouveauStatut);
        }

        // Appliquer la transition
        colis.changerStatut(nouveauStatut);
        Colis updated = colisRepository.save(colis);

        log.info("Statut du colis id={} changé : {} → {}", id, statutActuel, nouveauStatut);

        // TODO étape 17 — Publier l'événement Kafka
        // kafkaProducer.publishStatusChanged(new ColisStatusChangedEvent(...));

        return colisMapper.toDTO(updated);
    }

    // ─── Suppression ─────────────────────────────────────────────────────────

    @Override
    public void deleteColis(Long id, Long userId, String role) {
        log.info("Suppression du colis id={} par userId={}", id, userId);

        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ColisNotFoundException(id));

        // Seul un ADMIN peut supprimer
        if (!ROLE_ADMIN.equals(role)) {
            throw new AccessDeniedException("Seuls les administrateurs peuvent supprimer un colis.");
        }

        // Un colis ne peut être supprimé que s'il est EN_ATTENTE
        if (colis.getStatut() != StatutColis.EN_ATTENTE) {
            throw new IllegalStateException(
                "Un colis ne peut être supprimé qu'en statut EN_ATTENTE. Statut actuel : " + colis.getStatut()
            );
        }

        colisRepository.delete(colis);
        log.info("Colis id={} supprimé avec succès", id);
    }

    // ─── Méthodes privées ────────────────────────────────────────────────────

    /**
     * Génère un numéro de suivi unique au format COL-YYYYMMDD-XXXXX.
     * Vérifie l'unicité en BDD avec jusqu'à MAX_RETRY_SUIVI tentatives.
     *
     * @return un numéro de suivi garanti unique
     * @throws IllegalStateException si l'unicité ne peut pas être garantie après les tentatives
     */
    private String genererNumeroSuiviUnique() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (int tentative = 0; tentative < MAX_RETRY_SUIVI; tentative++) {
            String suffixe = genererSuffixeAleatoire(5);
            String candidat = TRACKING_PREFIX + "-" + date + "-" + suffixe;

            if (!colisRepository.existsByNumeroSuivi(candidat)) {
                log.debug("Numéro de suivi généré : {}", candidat);
                return candidat;
            }

            log.warn("Collision sur le numéro de suivi {} (tentative {}/{})", candidat, tentative + 1, MAX_RETRY_SUIVI);
        }

        throw new IllegalStateException("Impossible de générer un numéro de suivi unique après " + MAX_RETRY_SUIVI + " tentatives.");
    }

    /**
     * Génère une chaîne aléatoire de n caractères alphanumériques en majuscules.
     * Ex : "A3F7K"
     */
    private String genererSuffixeAleatoire(int longueur) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Vérifie qu'un utilisateur peut LIRE un colis.
     * ADMIN → accès total.
     * CLIENT → uniquement ses propres colis.
     */
    private void verifierAccesLecture(Colis colis, Long userId, String role) {
        if (!ROLE_ADMIN.equals(role) && !colis.getCreatedByUserId().equals(userId)) {
            log.warn("Accès refusé : userId={} tente d'accéder au colis id={} appartenant à userId={}",
                    userId, colis.getId(), colis.getCreatedByUserId());
            throw new AccessDeniedException("Vous n'avez pas accès à ce colis.");
        }
    }

    /**
     * Vérifie qu'un utilisateur peut MODIFIER un colis.
     * ADMIN → accès total.
     * CLIENT → uniquement ses propres colis.
     */
    private void verifierAccesEcriture(Colis colis, Long userId, String role) {
        if (!ROLE_ADMIN.equals(role) && !colis.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier ce colis.");
        }
    }
}