package com.gestiontests.service;

import com.gestiontests.entity.*;
import com.gestiontests.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class TestService {
    
    @Inject
    private SessionTestRepository sessionTestRepository;
    
    @Inject
    private SessionQuestionRepository sessionQuestionRepository;
    
    @Inject
    private ReponseCandidatRepository reponseCandidatRepository;
    
    @Inject
    private ReponsePossibleRepository reponsePossibleRepository;
    
    @Inject
    private QuestionRepository questionRepository;
    
    @Inject
    private ThemeRepository themeRepository;
    
    @Inject
    private CandidatService candidatService;
    
    @Inject
    private ParametreRepository parametreRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Inject
    private EmailService emailService;
    
    @Transactional
    public SessionTest demarrerTest(String codeSession) throws Exception {
        System.out.println("DEBUG: Démarrage du test pour codeSession: " + codeSession);
        
        // Vérifier si le candidat existe et peut passer le test
        Optional<Candidat> candidatOpt = candidatService.findByCodeSession(codeSession);
        if (candidatOpt.isEmpty()) {
            System.out.println("DEBUG: Code session invalide - candidat non trouvé");
            throw new Exception("Code session invalide");
        }
        
        Candidat candidat = candidatOpt.get();
        System.out.println("DEBUG: Candidat trouvé: " + candidat.getId() + ", validé: " + candidat.getEstValide());
        
        boolean peutPasser = candidatService.peutPasserTest(codeSession);
        System.out.println("DEBUG: Résultat peutPasserTest: " + peutPasser);
        
        if (!peutPasser) {
            System.out.println("DEBUG: Le candidat ne peut pas passer le test maintenant");
            throw new Exception("Vous ne pouvez pas passer le test maintenant");
        }
        
        // Vérifier si une session existe déjà pour ce codeSession
        Optional<SessionTest> sessionExistante = sessionTestRepository.findByCodeSession(codeSession);
        if (sessionExistante.isPresent() && !sessionExistante.get().getEstTermine()) {
            SessionTest session = sessionExistante.get();
            List<SessionQuestion> questions = sessionQuestionRepository.findBySession(session.getId());
            
            System.out.println("Returning existing active session: " + session.getId() + " with " + questions.size() + " questions");
            return session;
        }
        
        // Si une session terminée existe, on la réinitialise
        if (sessionExistante.isPresent() && sessionExistante.get().getEstTermine()) {
            System.out.println("Resetting terminated session: " + sessionExistante.get().getId());
            SessionTest session = sessionExistante.get();
            session.setEstTermine(false);
            session.setScoreTotal(0);
            session.setDateDebut(LocalDateTime.now());
            session.setDateFin(null);
            session.setPourcentage(BigDecimal.ZERO);
            
            // Supprimer anciennes réponses seulement (garder les questions existantes)
            List<SessionQuestion> questions = sessionQuestionRepository.findBySession(session.getId());
            System.out.println("Found " + questions.size() + " existing questions for terminated session");
            
            for (SessionQuestion q : questions) {
                List<ReponseCandidat> reponses = reponseCandidatRepository.findBySession(q.getId());
                for (ReponseCandidat r : reponses) {
                    reponseCandidatRepository.delete(r);
                }
            }
            
            session.setScoreMax(questions.size());
            SessionTest updatedSession = sessionTestRepository.update(session);
            System.out.println("Returning reset session with " + questions.size() + " existing questions");
            return updatedSession;
        }
        
        // Générer les questions pour le test
        List<SessionQuestion> questions = genererQuestionsPourTest();
        if (questions.isEmpty()) {
            throw new Exception("Aucune question disponible pour le test");
        }
        
        // Créer la session de test
        SessionTest sessionTest = new SessionTest();
        sessionTest.setCandidat(candidat);
        sessionTest.setCodeSession(codeSession);
        sessionTest.setScoreMax(questions.size());
        sessionTest.demarrerSession();
        
        SessionTest savedSession = sessionTestRepository.create(sessionTest);
        
        System.out.println("Created new session with ID: " + savedSession.getId());
        
        // Sauvegarder les questions de la session avec SQL natif
        for (int i = 0; i < questions.size(); i++) {
            SessionQuestion sessionQuestion = questions.get(i);
            
            // Utiliser SQL natif pour insérer avec les deux champs
            String sql = "INSERT INTO session_questions (ordre_affichage, id_question, id_session, id_session_test, temps_alloue) VALUES (?, ?, ?, ?, ?)";
            entityManager.createNativeQuery(sql)
                .setParameter(1, i + 1)
                .setParameter(2, sessionQuestion.getQuestion().getId())
                .setParameter(3, savedSession.getId())
                .setParameter(4, savedSession.getId())
                .setParameter(5, sessionQuestion.getTempsAlloue())
                .executeUpdate();
        }
        
        System.out.println("Created " + questions.size() + " session questions");
        
        return savedSession;
    }
    
    @Transactional
    public ReponseCandidat enregistrerReponse(Integer sessionId, Integer questionId, Map<String, Object> reponseData) throws Exception {
        Optional<SessionTest> sessionOpt = sessionTestRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session de test non trouvée");
        }
        
        SessionTest session = sessionOpt.get();
        if (session.getEstTermine()) {
            throw new Exception("Le test est déjà terminé");
        }
        
        // Vérifier si le temps n'est pas écoulé
        if (session.getDateDebut() != null) {
            LocalDateTime finEstimee = session.getDateDebut().plusMinutes(120); // 2 heures de test
            if (LocalDateTime.now().isAfter(finEstimee)) {
                terminerTest(sessionId);
                throw new Exception("Le temps du test est écoulé");
            }
        }
        
        Optional<SessionQuestion> sessionQuestionOpt = sessionQuestionRepository.findBySessionAndQuestion(sessionId, questionId);
        if (sessionQuestionOpt.isEmpty()) {
            throw new Exception("Question non trouvée dans cette session");
        }
        
        SessionQuestion sessionQuestion = sessionQuestionOpt.get();
        
        // Vérifier si une réponse existe déjà
        Optional<ReponseCandidat> reponseExistanteOpt = reponseCandidatRepository.findBySessionQuestion(sessionQuestion.getId());
        if (reponseExistanteOpt.isPresent()) {
            // Mettre à jour la réponse existante
            ReponseCandidat reponse = reponseExistanteOpt.get();
            mettreAJourReponse(reponse, reponseData);
            return reponseCandidatRepository.update(reponse);
        } else {
            // Créer une nouvelle réponse
            ReponseCandidat reponse = creerReponse(sessionQuestion, reponseData);
            return reponseCandidatRepository.create(reponse);
        }
    }
    
    @Transactional
    public SessionTest terminerTest(Integer sessionId) throws Exception {
        Optional<SessionTest> sessionOpt = sessionTestRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session de test non trouvée");
        }
        
        SessionTest session = sessionOpt.get();
        if (session.getEstTermine()) {
            return session;
        }
        
        // Calculer le score
        calculerScore(session);
        
        // Marquer comme terminé
        session.terminerSession();
        SessionTest updatedSession = sessionTestRepository.update(session);
        
        // Envoyer les résultats par email
        try {
            emailService.envoyerEmailResultats(
                session.getCandidat(),
                session.getScoreTotal().toString(),
                session.getPourcentage().toString()
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi des résultats par email: " + e.getMessage());
        }
        
        return updatedSession;
    }
    
    public Optional<SessionTest> getSessionById(Integer sessionId) {
        return sessionTestRepository.findById(sessionId);
    }
    
    public Optional<SessionTest> getSessionByCodeSession(String codeSession) {
        return sessionTestRepository.findByCodeSession(codeSession);
    }
    
    public List<SessionQuestion> getQuestionsBySession(Integer sessionId) {
        return sessionQuestionRepository.findBySession(sessionId);
    }
    
    public Optional<SessionQuestion> getNextQuestion(Integer sessionId, Integer currentQuestionId) {
        return sessionQuestionRepository.findNextQuestion(sessionId, currentQuestionId);
    }
    
    public Optional<SessionQuestion> getPreviousQuestion(Integer sessionId, Integer currentQuestionId) {
        return sessionQuestionRepository.findPreviousQuestion(sessionId, currentQuestionId);
    }
    
    public Optional<ReponseCandidat> getReponseBySessionQuestion(Integer sessionQuestionId) {
        return reponseCandidatRepository.findBySessionQuestion(sessionQuestionId);
    }
    
    public List<SessionTest> getSessionsByCandidat(Integer candidatId) {
        return sessionTestRepository.findByCandidat(candidatId);
    }
    
    public List<SessionTest> getAllSessions() {
        return sessionTestRepository.findAll();
    }
    
    public List<SessionTest> getRecentSessions(int limit) {
        return sessionTestRepository.findRecentSessions(limit);
    }
    
    private List<SessionQuestion> genererQuestionsPourTest() {
        // Récupérer les paramètres
        Integer nombreQuestionsParTheme = parametreRepository.getValeurParametreAsInteger("NOMBRE_QUESTIONS_PAR_THEME", 5);
        Integer tempsParQuestion = parametreRepository.getValeurParametreAsInteger("TEMPS_QUESTION_PAR_DEFAUT", 120);
        
        // Récupérer tous les thèmes
        List<Theme> themes = themeRepository.findAll();
        List<SessionQuestion> sessionQuestions = new ArrayList<>();
        
        Random random = new Random();
        
        for (Theme theme : themes) {
            // Récupérer les questions pour ce thème
            List<Question> questionsTheme = questionRepository.findByTheme(theme.getId());
            
            // Mélanger les questions
            Collections.shuffle(questionsTheme, random);
            
            // Prendre le nombre requis de questions
            int nombreAPrendre = Math.min(nombreQuestionsParTheme, questionsTheme.size());
            
            for (int i = 0; i < nombreAPrendre; i++) {
                Question question = questionsTheme.get(i);
                SessionQuestion sessionQuestion = new SessionQuestion();
                sessionQuestion.setQuestion(question);
                sessionQuestion.setTempsAlloue(tempsParQuestion);
                sessionQuestions.add(sessionQuestion);
            }
        }
        
        // Mélanger toutes les questions pour l'ordre aléatoire final
        Collections.shuffle(sessionQuestions, random);
        
        return sessionQuestions;
    }
    
    private void calculerScore(SessionTest session) {
        List<SessionQuestion> questions = sessionQuestionRepository.findBySession(session.getId());
        System.out.println("DEBUG: Calculating score for session " + session.getId() + " with " + questions.size() + " questions");
        
        int score = 0;
        int totalReponses = 0;
        
        for (SessionQuestion sessionQuestion : questions) {
            Optional<ReponseCandidat> reponseOpt = reponseCandidatRepository.findBySessionQuestion(sessionQuestion.getId());
            if (reponseOpt.isPresent()) {
                totalReponses++;
                ReponseCandidat reponse = reponseOpt.get();
                System.out.println("DEBUG: Question " + sessionQuestion.getQuestion().getId() + " - estCorrect: " + reponse.getEstCorrect());
                if (Boolean.TRUE.equals(reponse.getEstCorrect())) {
                    score++;
                }
            } else {
                System.out.println("DEBUG: Question " + sessionQuestion.getQuestion().getId() + " - No response found");
            }
        }
        
        System.out.println("DEBUG: Final score - Correct: " + score + ", Total responses: " + totalReponses + ", Total questions: " + questions.size());
        
        session.setScoreTotal(score);
        session.setScoreMax(questions.size());
    }
    
    private ReponseCandidat creerReponse(SessionQuestion sessionQuestion, Map<String, Object> reponseData) {
        ReponseCandidat reponse = new ReponseCandidat();
        reponse.setSessionQuestion(sessionQuestion);
        
        if (reponseData.containsKey("reponsePossibleId")) {
            // Réponse à choix
            Integer reponsePossibleId = (Integer) reponseData.get("reponsePossibleId");
            Optional<ReponsePossible> reponsePossibleOpt = reponsePossibleRepository.findById(reponsePossibleId);
            if (reponsePossibleOpt.isPresent()) {
                reponse.setReponsePossible(reponsePossibleOpt.get());
                reponse.setEstCorrect(reponsePossibleOpt.get().getEstCorrect());
            }
        } else if (reponseData.containsKey("reponseText")) {
            // Réponse textuelle
            reponse.setReponseText((String) reponseData.get("reponseText"));
            reponse.setEstCorrect(false); // Les réponses textuelles ne sont pas auto-évaluées
        }
        
        if (reponseData.containsKey("tempsReponse")) {
            reponse.setTempsReponse((Integer) reponseData.get("tempsReponse"));
        }
        
        return reponse;
    }
    
    private void mettreAJourReponse(ReponseCandidat reponse, Map<String, Object> reponseData) {
        if (reponseData.containsKey("reponsePossibleId")) {
            // Réponse à choix
            Integer reponsePossibleId = (Integer) reponseData.get("reponsePossibleId");
            Optional<ReponsePossible> reponsePossibleOpt = reponsePossibleRepository.findById(reponsePossibleId);
            if (reponsePossibleOpt.isPresent()) {
                reponse.setReponsePossible(reponsePossibleOpt.get());
                reponse.setEstCorrect(reponsePossibleOpt.get().getEstCorrect());
                reponse.setReponseText(null);
            }
        } else if (reponseData.containsKey("reponseText")) {
            reponse.setReponseText((String) reponseData.get("reponseText"));
            reponse.setReponsePossible(null);
            reponse.setEstCorrect(false); // Les réponses textuelles ne sont pas auto-évaluées
        }
        
        if (reponseData.containsKey("tempsReponse")) {
            reponse.setTempsReponse((Integer) reponseData.get("tempsReponse"));
        }
    }
    
    public boolean peutNaviguerVersQuestion(Integer sessionId, Integer questionId) throws Exception {
        Optional<SessionTest> sessionOpt = sessionTestRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        SessionTest session = sessionOpt.get();
        if (session.getEstTermine()) {
            return false;
        }
        
        // Vérifier si la question appartient à cette session
        Optional<SessionQuestion> sessionQuestionOpt = sessionQuestionRepository.findBySessionAndQuestion(sessionId, questionId);
        return sessionQuestionOpt.isPresent();
    }
    
    public long getTempsRestant(Integer sessionId) {
        Optional<SessionTest> sessionOpt = sessionTestRepository.findById(sessionId);
        if (sessionOpt.isEmpty() || sessionOpt.get().getDateDebut() == null) {
            return 0;
        }
        
        SessionTest session = sessionOpt.get();
        LocalDateTime finEstimee = session.getDateDebut().plusMinutes(120); // 2 heures de test
        LocalDateTime maintenant = LocalDateTime.now();
        
        if (maintenant.isAfter(finEstimee)) {
            return 0;
        }
        
        return java.time.Duration.between(maintenant, finEstimee).getSeconds();
    }
}
