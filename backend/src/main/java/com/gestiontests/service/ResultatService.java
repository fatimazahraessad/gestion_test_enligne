package com.gestiontests.service;

import com.gestiontests.entity.*;
import com.gestiontests.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResultatService {
    
    @Inject
    private SessionTestRepository sessionTestRepository;
    
    @Inject
    private ReponseCandidatRepository reponseCandidatRepository;
    
    @Inject
    private CandidatRepository candidatRepository;
    
    @Inject
    private QuestionRepository questionRepository;
    
    @Inject
    private ThemeRepository themeRepository;
    
    @Inject
    private ReponsePossibleRepository reponsePossibleRepository;
    
    public List<SessionTest> getResultatsByCandidat(Integer candidatId) {
        return sessionTestRepository.findByCandidat(candidatId);
    }
    
    public List<SessionTest> getResultatsTermineesByCandidat(Integer candidatId) {
        return sessionTestRepository.findByCandidat(candidatId).stream()
            .filter(SessionTest::getEstTermine)
            .collect(Collectors.toList());
    }
    
    public List<SessionTest> rechercherSessionsParCandidat(String terme) {
        List<Candidat> candidats = candidatRepository.findByNomOrPrenomOrEcole(terme);
        List<SessionTest> sessions = new ArrayList<>();
        
        for (Candidat candidat : candidats) {
            sessions.addAll(sessionTestRepository.findByCandidat(candidat.getId()));
        }
        
        return sessions.stream()
            .sorted((s1, s2) -> s2.getDateDebut().compareTo(s1.getDateDebut()))
            .collect(Collectors.toList());
    }
    
    public List<SessionTest> rechercheAvanceeSessions(String nom, String prenom, String ecole, String codeExam) {
        List<SessionTest> sessions = new ArrayList<>();
        
        if (codeExam != null && !codeExam.trim().isEmpty()) {
            Optional<SessionTest> sessionOpt = sessionTestRepository.findByCodeSession(codeExam);
            if (sessionOpt.isPresent()) {
                sessions.add(sessionOpt.get());
            }
        } else {
            List<Candidat> candidats = new ArrayList<>();
            
            if (nom != null && !nom.trim().isEmpty()) {
                candidats.addAll(candidatRepository.findByNomOrPrenomOrEcole(nom));
            }
            if (prenom != null && !prenom.trim().isEmpty()) {
                candidats.addAll(candidatRepository.findByNomOrPrenomOrEcole(prenom));
            }
            if (ecole != null && !ecole.trim().isEmpty()) {
                candidats.addAll(candidatRepository.findByEcole(ecole));
            }
            
            // Éviter les doublons
            Set<Integer> candidatIds = candidats.stream()
                .map(Candidat::getId)
                .collect(Collectors.toSet());
            
            for (Integer candidatId : candidatIds) {
                sessions.addAll(sessionTestRepository.findByCandidat(candidatId));
            }
        }
        
        return sessions.stream()
            .sorted((s1, s2) -> s2.getDateDebut().compareTo(s1.getDateDebut()))
            .distinct()
            .collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getStatsParEcole() {
        List<Candidat> candidats = candidatRepository.findAll();
        Map<String, List<Candidat>> candidatsParEcole = candidats.stream()
            .filter(c -> c.getEcole() != null && !c.getEcole().trim().isEmpty())
            .collect(Collectors.groupingBy(Candidat::getEcole));
        
        List<Map<String, Object>> stats = new ArrayList<>();
        
        for (Map.Entry<String, List<Candidat>> entry : candidatsParEcole.entrySet()) {
            String ecole = entry.getKey();
            List<Candidat> candidatsEcole = entry.getValue();
            
            List<SessionTest> sessionsEcole = new ArrayList<>();
            for (Candidat candidat : candidatsEcole) {
                sessionsEcole.addAll(sessionTestRepository.findByCandidat(candidat.getId()));
            }
            
            List<SessionTest> sessionsTerminees = sessionsEcole.stream()
                .filter(SessionTest::getEstTermine)
                .collect(Collectors.toList());
            
            double scoreMoyen = sessionsTerminees.stream()
                .mapToDouble(s -> s.getPourcentage().doubleValue())
                .average()
                .orElse(0.0);
            
            Map<String, Object> statEcole = new HashMap<>();
            statEcole.put("ecole", ecole);
            statEcole.put("nombreCandidats", candidatsEcole.size());
            statEcole.put("nombreSessions", sessionsEcole.size());
            statEcole.put("sessionsTerminees", sessionsTerminees.size());
            statEcole.put("scoreMoyen", Math.round(scoreMoyen * 100.0) / 100.0);
            
            stats.add(statEcole);
        }
        
        // Trier par score moyen décroissant
        stats.sort((s1, s2) -> Double.compare(
            (Double) s2.get("scoreMoyen"), 
            (Double) s1.get("scoreMoyen")
        ));
        
        return stats;
    }
    
    public List<Map<String, Object>> getStatsParDate(Integer jours) {
        LocalDate dateFin = LocalDate.now();
        LocalDate dateDebut = dateFin.minusDays(jours);
        
        List<SessionTest> sessions = sessionTestRepository.findByDateRange(
            dateDebut.atStartOfDay(), 
            dateFin.atTime(23, 59, 59)
        );
        
        Map<LocalDate, List<SessionTest>> sessionsParDate = sessions.stream()
            .filter(s -> s.getDateDebut() != null)
            .collect(Collectors.groupingBy(s -> s.getDateDebut().toLocalDate()));
        
        List<Map<String, Object>> stats = new ArrayList<>();
        
        for (LocalDate date = dateDebut; !date.isAfter(dateFin); date = date.plusDays(1)) {
            List<SessionTest> sessionsDate = sessionsParDate.getOrDefault(date, new ArrayList<>());
            List<SessionTest> sessionsTerminees = sessionsDate.stream()
                .filter(SessionTest::getEstTermine)
                .collect(Collectors.toList());
            
            double scoreMoyen = sessionsTerminees.stream()
                .mapToDouble(s -> s.getPourcentage().doubleValue())
                .average()
                .orElse(0.0);
            
            Map<String, Object> statDate = new HashMap<>();
            statDate.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            statDate.put("nombreSessions", sessionsDate.size());
            statDate.put("sessionsTerminees", sessionsTerminees.size());
            statDate.put("scoreMoyen", Math.round(scoreMoyen * 100.0) / 100.0);
            
            stats.add(statDate);
        }
        
        return stats;
    }
    
    public List<SessionTest> getTopScores(Integer limit) {
        return sessionTestRepository.findTopScorers(limit);
    }
    
    public String exporterResultatsCSV(String dateDebutStr, String dateFinStr) throws Exception {
        LocalDateTime dateDebut = dateDebutStr != null ? 
            LocalDate.parse(dateDebutStr).atStartOfDay() : 
            LocalDateTime.now().minusDays(30);
        LocalDateTime dateFin = dateFinStr != null ? 
            LocalDate.parse(dateFinStr).atTime(23, 59, 59) : 
            LocalDateTime.now();
        
        List<SessionTest> sessions = sessionTestRepository.findByDateRange(dateDebut, dateFin);
        List<SessionTest> sessionsTerminees = sessions.stream()
            .filter(SessionTest::getEstTermine)
            .sorted((s1, s2) -> s2.getDateDebut().compareTo(s1.getDateDebut()))
            .collect(Collectors.toList());
        
        StringBuilder csv = new StringBuilder();
        csv.append("Nom,Prénom,École,Filière,Email,Date Test,Score,Score Max,Pourcentage,Code Session\n");
        
        for (SessionTest session : sessionsTerminees) {
            Candidat candidat = session.getCandidat();
            csv.append(candidat.getNom()).append(",");
            csv.append(candidat.getPrenom()).append(",");
            csv.append(candidat.getEcole()).append(",");
            csv.append(candidat.getFiliere() != null ? candidat.getFiliere() : "").append(",");
            csv.append(candidat.getEmail()).append(",");
            csv.append(session.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append(",");
            csv.append(session.getScoreTotal()).append(",");
            csv.append(session.getScoreMax()).append(",");
            csv.append(session.getPourcentage()).append(",");
            csv.append(session.getCodeSession()).append("\n");
        }
        
        return csv.toString();
    }
    
    public Map<String, Object> getDetailsSession(Integer sessionId) {
        Optional<SessionTest> sessionOpt = sessionTestRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }
        
        SessionTest session = sessionOpt.get();
        List<ReponseCandidat> reponses = reponseCandidatRepository.findBySession(sessionId);
        
        // Statistiques par thème
        Map<String, Object> statsParTheme = new HashMap<>();
        List<Theme> themes = themeRepository.findAll();
        
        for (Theme theme : themes) {
            List<ReponseCandidat> reponsesTheme = reponses.stream()
                .filter(r -> r.getSessionQuestion().getQuestion().getTheme().getId().equals(theme.getId()))
                .collect(Collectors.toList());
            
            long correctes = reponsesTheme.stream()
                .mapToLong(r -> r.getEstCorrect() ? 1 : 0)
                .sum();
            
            Map<String, Object> statTheme = new HashMap<>();
            statTheme.put("total", reponsesTheme.size());
            statTheme.put("correctes", correctes);
            statTheme.put("pourcentage", reponsesTheme.size() > 0 ? 
                Math.round((double) correctes / reponsesTheme.size() * 10000.0) / 100.0 : 0.0);
            
            statsParTheme.put(theme.getNom(), statTheme);
        }
        
        // Temps moyen par question
        double tempsMoyen = reponses.stream()
            .filter(r -> r.getTempsReponse() != null)
            .mapToInt(ReponseCandidat::getTempsReponse)
            .average()
            .orElse(0.0);
        
        // Questions sans réponse
        long questionsSansReponse = session.getSessionQuestions().stream()
            .mapToLong(sq -> reponses.stream()
                .anyMatch(r -> r.getSessionQuestion().getId().equals(sq.getId())) ? 0 : 1)
            .sum();
        
        Map<String, Object> details = new HashMap<>();
        details.put("statsParTheme", statsParTheme);
        details.put("tempsMoyen", Math.round(tempsMoyen * 100.0) / 100.0);
        details.put("questionsSansReponse", questionsSansReponse);
        details.put("totalQuestions", session.getSessionQuestions().size());
        details.put("reponsesDonnees", reponses.size());
        
        return details;
    }
    
    public Map<String, Object> getStatsGlobales() {
        List<SessionTest> sessions = sessionTestRepository.findRecentSessions(1000);
        
        long total = sessions.size();
        long terminees = sessions.stream().mapToLong(s -> s.getEstTermine() ? 1 : 0).sum();
        double scoreMoyen = sessions.stream()
            .filter(SessionTest::getEstTermine)
            .mapToDouble(s -> s.getPourcentage().doubleValue())
            .average()
            .orElse(0.0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", total);
        stats.put("sessionsTerminees", terminees);
        stats.put("tauxCompletion", total > 0 ? (double) terminees / total * 100 : 0);
        stats.put("scoreMoyen", Math.round(scoreMoyen * 100.0) / 100.0);
        
        return stats;
    }
}
