package com.gestiontests.service;

import com.gestiontests.entity.Candidat;
import com.gestiontests.entity.CreneauHoraire;
import com.gestiontests.entity.Inscription;
import com.gestiontests.repository.CandidatRepository;
import com.gestiontests.repository.CreneauHoraireRepository;
import com.gestiontests.repository.InscriptionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@ApplicationScoped
public class CandidatService {
    
    @Inject
    private CandidatRepository candidatRepository;
    
    @Inject
    private CreneauHoraireRepository creneauHoraireRepository;
    
    @Inject
    private InscriptionRepository inscriptionRepository;
    
    @Inject
    private EmailService emailService;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    
    @Transactional
    public Candidat inscrireCandidat(Candidat candidat, Integer creneauId) throws Exception {
        // Vérifier si l'email existe déjà
        if (candidatRepository.existsByEmail(candidat.getEmail())) {
            throw new Exception("Un candidat avec cet email existe déjà");
        }
        
        // Vérifier si le créneau existe et est disponible
        Optional<CreneauHoraire> creneauOpt = creneauHoraireRepository.findById(creneauId);
        if (creneauOpt.isEmpty()) {
            throw new Exception("Le créneau horaire n'existe pas");
        }
        
        CreneauHoraire creneau = creneauOpt.get();
        if (creneau.getEstComplet()) {
            throw new Exception("Le créneau horaire est complet");
        }
        
        // Le candidat est créé sans code session pour l'instant
        candidat.setEstValide(false);
        candidat.setCodeSession(null); // Pas de code session avant validation
        
        // Sauvegarder le candidat
        Candidat savedCandidat = candidatRepository.create(candidat);
        
        // Créer l'inscription
        Inscription inscription = new Inscription(savedCandidat, creneau);
        inscriptionRepository.create(inscription);
        
        // Envoyer l'email de confirmation d'inscription (en attente de validation)
        try {
            emailService.envoyerEmailInscription(savedCandidat, creneau, null);
        } catch (Exception e) {
            // Logger l'erreur mais ne pas bloquer l'inscription
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
        
        return savedCandidat;
    }
    
    @Transactional
    public Candidat validerInscription(Integer candidatId) throws Exception {
        Optional<Candidat> candidatOpt = candidatRepository.findById(candidatId);
        if (candidatOpt.isEmpty()) {
            throw new Exception("Candidat non trouvé");
        }
        
        Candidat candidat = candidatOpt.get();
        
        // Générer le code session uniquement lors de la validation
        String codeSession = generateUniqueCodeSession();
        candidat.setCodeSession(codeSession);
        candidat.setEstValide(true);
        
        // Envoyer l'email de validation avec le code session
        try {
            emailService.envoyerEmailValidation(candidat);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de validation: " + e.getMessage());
        }
        
        return candidatRepository.update(candidat);
    }
    
    public Optional<Candidat> findByEmail(String email) {
        return candidatRepository.findByEmail(email);
    }
    
    @Transactional
    public Optional<Candidat> findByCodeSession(String codeSession) {
        return candidatRepository.findByCodeSession(codeSession);
    }
    
    public List<Candidat> findByNomOrPrenomOrEcole(String searchTerm) {
        return candidatRepository.findByNomOrPrenomOrEcole(searchTerm);
    }
    
    public List<Candidat> findByEstValide(Boolean estValide) {
        return candidatRepository.findByEstValide(estValide);
    }
    
    public List<Candidat> findRecentCandidates(int limit) {
        return candidatRepository.findRecentCandidates(limit);
    }
    
    @Transactional
    public Candidat updateCandidat(Candidat candidat) {
        return candidatRepository.update(candidat);
    }
    
    @Transactional
    public void deleteCandidat(Integer candidatId) {
        candidatRepository.deleteById(candidatId);
    }
    
    public List<Candidat> findAll() {
        return candidatRepository.findAll();
    }
    
    public Optional<Candidat> findById(Integer id) {
        return candidatRepository.findById(id);
    }
    
    public long count() {
        return candidatRepository.count();
    }
    
    /**
     * Génère un code session unique de 8 caractères
     */
    private String generateUniqueCodeSession() {
        Random random = new Random();
        StringBuilder code;
        int attempts = 0;
        final int maxAttempts = 100;
        
        do {
            code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Impossible de générer un code session unique après " + maxAttempts + " tentatives");
            }
        } while (candidatRepository.existsByCodeSession(code.toString()));
        
        return code.toString();
    }
    
    /**
     * Vérifie si un candidat peut passer un test maintenant
     */
    @Transactional
    public boolean peutPasserTest(String codeSession) {
        Optional<Candidat> candidatOpt = candidatRepository.findByCodeSession(codeSession);
        if (candidatOpt.isEmpty()) {
            System.out.println("DEBUG: Candidat non trouvé pour le code session: " + codeSession);
            return false;
        }
        
        Candidat candidat = candidatOpt.get();
        
        // Vérifier si le candidat est validé
        if (!candidat.getEstValide()) {
            System.out.println("DEBUG: Candidat non validé: " + candidat.getId());
            return false;
        }
        
        // Vérifier si le candidat a une inscription à un créneau
        List<Inscription> inscriptions = inscriptionRepository.findByCandidat(candidat.getId());
        if (inscriptions.isEmpty()) {
            System.out.println("DEBUG: Aucune inscription trouvée pour le candidat: " + candidat.getId());
            return false;
        }
        
        // Vérifier si le créneau horaire est valide
        LocalDateTime maintenant = LocalDateTime.now();
        System.out.println("DEBUG: Validation créneau pour candidat " + candidat.getId() + " à " + maintenant);
        boolean creneauValide = false;
        
        for (Inscription inscription : inscriptions) {
            CreneauHoraire creneau = inscription.getCreneau();
            System.out.println("DEBUG: Créneau trouvé - Date: " + creneau.getDateExam() + ", Début: " + creneau.getHeureDebut() + ", Fin: " + creneau.getHeureFin());
            
            // Créer les LocalDateTime complets pour le créneau en utilisant la date d'examen
            LocalDateTime debutCreneau = LocalDateTime.of(
                creneau.getDateExam(), 
                creneau.getHeureDebut()
            );
            LocalDateTime finCreneau = LocalDateTime.of(
                creneau.getDateExam(), 
                creneau.getHeureFin()
            );
            
            // Si le créneau est pour aujourd'hui, utiliser la date actuelle
            // Si le créneau est pour un autre jour, vérifier si ce jour est aujourd'hui
            if (debutCreneau.toLocalDate().equals(maintenant.toLocalDate())) {
                // Créneau pour aujourd'hui - utiliser les heures exactes
                debutCreneau = LocalDateTime.of(
                    maintenant.toLocalDate(),
                    creneau.getHeureDebut()
                );
                finCreneau = LocalDateTime.of(
                    maintenant.toLocalDate(),
                    creneau.getHeureFin()
                );
            } else if (maintenant.toLocalDate().isAfter(debutCreneau.toLocalDate())) {
                // Créneau pour un jour passé - pas valide
                System.out.println("DEBUG: Créneau dans le passé - " + debutCreneau.toLocalDate());
                continue;
            }
            
            System.out.println("DEBUG: Début créneau complet: " + debutCreneau);
            System.out.println("DEBUG: Fin créneau complet: " + finCreneau);
            
            // Vérifier si le créneau n'est pas encore atteint (trop tôt)
            if (maintenant.isBefore(debutCreneau)) {
                System.out.println("DEBUG: Trop tôt - " + maintenant + " < " + debutCreneau);
                continue; // Continuer à vérifier d'autres créneaux
            }
            
            // Vérifier si la durée du test est dépassée (trop tard)
            // Utiliser directement l'heure de fin du créneau (pas +2h)
            if (maintenant.isAfter(finCreneau)) {
                System.out.println("DEBUG: Trop tard - " + maintenant + " > " + finCreneau);
                continue; // Continuer à vérifier d'autres créneaux
            }
            
            System.out.println("DEBUG: Créneau valide pour cette inscription");
            creneauValide = true;
            break; // Sortir dès qu'on trouve un créneau valide
        }
        
        System.out.println("DEBUG: Résultat final pour candidat " + candidat.getId() + ": " + creneauValide);
        return creneauValide;
    }
    
    /**
     * Vérifie si le créneau horaire est passé
     */
    @Transactional
    public boolean creneauEstPasse(String codeSession) {
        Optional<Candidat> candidatOpt = candidatRepository.findByCodeSession(codeSession);
        if (candidatOpt.isEmpty()) {
            return true; // Considérer comme passé si le candidat n'existe pas
        }
        
        List<Inscription> inscriptions = inscriptionRepository.findByCandidat(candidatOpt.get().getId());
        if (inscriptions.isEmpty()) {
            return true;
        }
        
        for (Inscription inscription : inscriptions) {
            CreneauHoraire creneau = inscription.getCreneau();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime finCreneau = LocalDateTime.of(creneau.getDateExam(), creneau.getHeureFin());
            
            if (now.isAfter(finCreneau)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Vérifie si le créneau horaire est atteint (peut commencer le test)
     */
    @Transactional
    public boolean creneauEstAtteint(String codeSession) {
        Optional<Candidat> candidatOpt = candidatRepository.findByCodeSession(codeSession);
        if (candidatOpt.isEmpty()) {
            return false;
        }
        
        List<Inscription> inscriptions = inscriptionRepository.findByCandidat(candidatOpt.get().getId());
        if (inscriptions.isEmpty()) {
            return false;
        }
        
        for (Inscription inscription : inscriptions) {
            CreneauHoraire creneau = inscription.getCreneau();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime debutCreneau = LocalDateTime.of(creneau.getDateExam(), creneau.getHeureDebut());
            
            if (now.isAfter(debutCreneau)) {
                return true;
            }
        }
        
        return false;
    }
}
