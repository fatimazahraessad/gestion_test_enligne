package com.gestiontests.rest;

import com.gestiontests.entity.SessionTest;
import com.gestiontests.entity.ReponseCandidat;
import com.gestiontests.service.TestService;
import com.gestiontests.service.ResultatService;
import com.gestiontests.repository.ReponseCandidatRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/resultats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ResultatResource {
    
    // DTO pour éviter les problèmes de lazy loading
    public static class SessionTestDTO {
        private Integer id;
        private String codeSession;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private Boolean estTermine;
        private Integer scoreTotal;
        private Integer scoreMax;
        private String pourcentage;
        
        public SessionTestDTO(SessionTest session) {
            this.id = session.getId();
            this.codeSession = session.getCodeSession();
            this.dateDebut = session.getDateDebut();
            this.dateFin = session.getDateFin();
            this.estTermine = session.getEstTermine();
            this.scoreTotal = session.getScoreTotal();
            this.scoreMax = session.getScoreMax();
            this.pourcentage = session.getPourcentage() != null ? session.getPourcentage().toString() : "0.00";
        }
        
        // Getters
        public Integer getId() { return id; }
        public String getCodeSession() { return codeSession; }
        public LocalDateTime getDateDebut() { return dateDebut; }
        public LocalDateTime getDateFin() { return dateFin; }
        public Boolean getEstTermine() { return estTermine; }
        public Integer getScoreTotal() { return scoreTotal; }
        public Integer getScoreMax() { return scoreMax; }
        public String getPourcentage() { return pourcentage; }
    }
    
    @Inject
    private ReponseCandidatRepository reponseCandidatRepository;
    
    @Inject
    private TestService testService;
    
    @Inject
    private ResultatService resultatService;
    
    @GET
    @Path("/session/{sessionId}")
    public Response getResultatsBySession(@PathParam("sessionId") Integer sessionId) {
        Optional<SessionTest> sessionOpt = testService.getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session de test non trouvée"))
                .build();
        }
        
        SessionTest session = sessionOpt.get();
        List<ReponseCandidat> reponses = reponseCandidatRepository.findBySession(sessionId);
        
        return Response.ok(Map.of(
            "session", session,
            "reponses", reponses,
            "score", Map.of(
                "total", session.getScoreTotal(),
                "max", session.getScoreMax(),
                "pourcentage", session.getPourcentage()
            )
        )).build();
    }
    
    @GET
    @Path("/candidat/{candidatId}")
    public Response getResultatsByCandidat(@PathParam("candidatId") Integer candidatId) {
        List<SessionTest> sessions = testService.getSessionsByCandidat(candidatId);
        List<SessionTestDTO> sessionDTOs = sessions.stream()
            .map(SessionTestDTO::new)
            .collect(Collectors.toList());
        
        return Response.ok(Map.of(
            "candidatId", candidatId,
            "sessions", sessionDTOs,
            "totalSessions", sessionDTOs.size(),
            "sessionsTerminees", sessionDTOs.stream().mapToLong(s -> s.getEstTermine() ? 1 : 0).sum()
        )).build();
    }
    
    @GET
    @Path("/candidat/{candidatId}/terminees")
    public Response getResultatsTermineesByCandidat(@PathParam("candidatId") Integer candidatId) {
        List<SessionTest> sessions = testService.getSessionsByCandidat(candidatId);
        List<SessionTest> sessionsTerminees = sessions.stream()
            .filter(SessionTest::getEstTermine)
            .toList();
        
        return Response.ok(Map.of(
            "candidatId", candidatId,
            "sessions", sessionsTerminees,
            "total", sessionsTerminees.size()
        )).build();
    }
    
    @GET
    @Path("/recherche")
    public Response rechercherResultats(@QueryParam("terme") String terme) {
        if (terme == null || terme.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Le terme de recherche est obligatoire"))
                .build();
        }
        
        List<SessionTest> sessions = resultatService.rechercherSessionsParCandidat(terme);
        
        return Response.ok(Map.of(
            "terme", terme,
            "sessions", sessions,
            "total", sessions.size()
        )).build();
    }
    
    @GET
    @Path("/recherche/avancee")
    public Response rechercheAvancee(@QueryParam("nom") String nom,
                                   @QueryParam("prenom") String prenom,
                                   @QueryParam("ecole") String ecole,
                                   @QueryParam("codeExam") String codeExam) {
        List<SessionTest> sessions = resultatService.rechercheAvanceeSessions(nom, prenom, ecole, codeExam);
        
        return Response.ok(Map.of(
            "critères", Map.of(
                "nom", nom,
                "prenom", prenom,
                "ecole", ecole,
                "codeExam", codeExam
            ),
            "sessions", sessions,
            "total", sessions.size()
        )).build();
    }
    
    @GET
    @Path("/stats/globales")
    public Response getStatsGlobales() {
        List<SessionTest> recentSessions = testService.getRecentSessions(1000);
        
        long totalSessions = recentSessions.size();
        long sessionsTerminees = recentSessions.stream().mapToLong(s -> s.getEstTermine() ? 1 : 0).sum();
        double scoreMoyen = recentSessions.stream()
            .filter(SessionTest::getEstTermine)
            .mapToDouble(s -> s.getPourcentage().doubleValue())
            .average()
            .orElse(0.0);
        
        double scoreMax = recentSessions.stream()
            .filter(SessionTest::getEstTermine)
            .mapToDouble(s -> s.getPourcentage().doubleValue())
            .max()
            .orElse(0.0);
        
        double scoreMin = recentSessions.stream()
            .filter(SessionTest::getEstTermine)
            .mapToDouble(s -> s.getPourcentage().doubleValue())
            .min()
            .orElse(0.0);
        
        return Response.ok(Map.of(
            "totalSessions", totalSessions,
            "sessionsTerminees", sessionsTerminees,
            "tauxCompletion", totalSessions > 0 ? (double) sessionsTerminees / totalSessions * 100 : 0,
            "scoreMoyen", Math.round(scoreMoyen * 100.0) / 100.0,
            "scoreMax", Math.round(scoreMax * 100.0) / 100.0,
            "scoreMin", Math.round(scoreMin * 100.0) / 100.0
        )).build();
    }
    
    @GET
    @Path("/stats/par-ecole")
    public Response getStatsParEcole() {
        List<Map<String, Object>> statsEcole = resultatService.getStatsParEcole();
        
        return Response.ok(Map.of("statsEcole", statsEcole)).build();
    }
    
    @GET
    @Path("/stats/par-date")
    public Response getStatsParDate(@QueryParam("jours") Integer jours) {
        if (jours == null) {
            jours = 30; // Par défaut 30 jours
        }
        
        List<Map<String, Object>> statsDate = resultatService.getStatsParDate(jours);
        
        return Response.ok(Map.of(
            "periode", jours + " jours",
            "statsDate", statsDate
        )).build();
    }
    
    @GET
    @Path("/top-scores/{limit}")
    public Response getTopScores(@PathParam("limit") Integer limit) {
        List<SessionTest> topScores = resultatService.getTopScores(limit);
        
        return Response.ok(Map.of(
            "topScores", topScores,
            "limit", limit
        )).build();
    }
    
    @GET
    @Path("/recent/{limit}")
    public Response getResultatsRecents(@PathParam("limit") Integer limit) {
        List<SessionTest> sessions = testService.getRecentSessions(limit);
        
        return Response.ok(Map.of(
            "sessions", sessions,
            "limit", limit
        )).build();
    }
    
    @GET
    @Path("/export/csv")
    @Produces("text/csv")
    public Response exportResultatsCSV(@QueryParam("dateDebut") String dateDebut,
                                      @QueryParam("dateFin") String dateFin) {
        try {
            String csv = resultatService.exporterResultatsCSV(dateDebut, dateFin);
            
            return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"resultats.csv\"")
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/session/{sessionId}/details")
    public Response getDetailsSession(@PathParam("sessionId") Integer sessionId) {
        Optional<SessionTest> sessionOpt = testService.getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Session de test non trouvée"))
                .build();
        }
        
        SessionTest session = sessionOpt.get();
        Map<String, Object> details = resultatService.getDetailsSession(sessionId);
        
        return Response.ok(Map.of(
            "session", session,
            "details", details
        )).build();
    }
    
    @GET
    @Path("/candidat/{candidatId}/progression")
    public Response getProgressionCandidat(@PathParam("candidatId") Integer candidatId) {
        List<SessionTest> sessions = testService.getSessionsByCandidat(candidatId);
        List<SessionTest> sessionsTerminees = sessions.stream()
            .filter(SessionTest::getEstTermine)
            .sorted((s1, s2) -> s1.getDateDebut().compareTo(s2.getDateDebut()))
            .toList();
        
        return Response.ok(Map.of(
            "candidatId", candidatId,
            "progression", sessionsTerminees,
            "total", sessionsTerminees.size(),
            "amelioration", calculerAmelioration(sessionsTerminees)
        )).build();
    }
    
    private Map<String, Object> calculerAmelioration(List<SessionTest> sessions) {
        if (sessions.size() < 2) {
            return Map.of("amelioration", 0.0, "tendance", "insuffisant_de_donnees");
        }
        
        double premierScore = sessions.get(0).getPourcentage().doubleValue();
        double dernierScore = sessions.get(sessions.size() - 1).getPourcentage().doubleValue();
        double amelioration = dernierScore - premierScore;
        
        String tendance;
        if (amelioration > 5) {
            tendance = "amelioration";
        } else if (amelioration < -5) {
            tendance = "regression";
        } else {
            tendance = "stable";
        }
        
        return Map.of(
            "amelioration", Math.round(amelioration * 100.0) / 100.0,
            "tendance", tendance,
            "premierScore", premierScore,
            "dernierScore", dernierScore
        );
    }
}
