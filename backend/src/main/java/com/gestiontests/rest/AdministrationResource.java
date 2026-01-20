package com.gestiontests.rest;

import com.gestiontests.entity.*;
import com.gestiontests.service.*;
import com.gestiontests.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdministrationResource {
    
    @Inject
    private QuestionService questionService;
    
    @Inject
    private ParametreService parametreService;
    
    @Inject
    private CandidatService candidatService;
    
    @Inject
    private CreneauHoraireService creneauHoraireService;
    
    @Inject
    private EmailService emailService;
    
    @Inject
    private ResultatService resultatService;
    
    @Inject
    private AdministrateurRepository administrateurRepository;
    
    @Inject
    private TestService testService;
    
    // Login administrateur
    @POST
    @Path("/login")
    public Response loginAdmin(Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            if (username == null || password == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Nom d'utilisateur et mot de passe requis"))
                    .build();
            }
            
            Optional<Administrateur> adminOpt = administrateurRepository.findByUsername(username);
            if (adminOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Nom d'utilisateur ou mot de passe incorrect"))
                    .build();
            }
            
            Administrateur admin = adminOpt.get();
            // Temporairement pour test : comparaison en clair
            if (!password.equals(admin.getPassword())) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Nom d'utilisateur ou mot de passe incorrect"))
                    .build();
            }
            
            // Ne pas renvoyer le mot de passe
            admin.setPassword(null);
            
            return Response.ok(Map.of(
                "message", "Connexion réussie",
                "admin", admin
            )).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erreur lors de la connexion"))
                .build();
        }
    }
    
    // Gestion des questions
    @POST
    @Path("/questions")
    public Response createQuestion(Map<String, Object> questionData) {
        try {
            Question question = questionService.createQuestion(questionData);
            return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "message", "Question créée avec succès",
                    "question", question
                ))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/questions/{id}")
    public Response updateQuestion(@PathParam("id") Integer id, Map<String, Object> questionData) {
        try {
            Question question = questionService.updateQuestion(id, questionData);
            return Response.ok(Map.of(
                "message", "Question mise à jour avec succès",
                "question", question
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @DELETE
    @Path("/questions/{id}")
    public Response deleteQuestion(@PathParam("id") Integer id) {
        try {
            questionService.deleteQuestion(id);
            return Response.ok(Map.of("message", "Question supprimée avec succès")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/questions")
    public Response getAllQuestions() {
        try {
            List<Question> questions = questionService.findAllWithDetails();
            
            // Créer une liste de questions simplifiées pour éviter les problèmes de sérialisation
            List<Map<String, Object>> questionsData = questions.stream().map(q -> {
                Map<String, Object> qMap = new java.util.HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("libelle", q.getLibelle());
                qMap.put("explication", q.getExplication());
                // Formater la date pour éviter les problèmes de sérialisation
                if (q.getCreatedAt() != null) {
                    qMap.put("createdAt", q.getCreatedAt().toString());
                }
                
                // Ajouter les informations de thème
                if (q.getTheme() != null) {
                    Map<String, Object> themeMap = new java.util.HashMap<>();
                    themeMap.put("id", q.getTheme().getId());
                    themeMap.put("nom", q.getTheme().getNom());
                    qMap.put("theme", themeMap);
                }
                
                // Ajouter les informations de type
                if (q.getTypeQuestion() != null) {
                    Map<String, Object> typeMap = new java.util.HashMap<>();
                    typeMap.put("id", q.getTypeQuestion().getId());
                    typeMap.put("nom", q.getTypeQuestion().getNom());
                    qMap.put("typeQuestion", typeMap);
                }
                
                // Ajouter les réponses
                if (q.getReponsesPossibles() != null) {
                    List<Map<String, Object>> reponsesData = q.getReponsesPossibles().stream().map(r -> {
                        Map<String, Object> rMap = new java.util.HashMap<>();
                        rMap.put("id", r.getId());
                        rMap.put("libelle", r.getLibelle());
                        rMap.put("estCorrect", r.getEstCorrect());
                        return rMap;
                    }).collect(java.util.stream.Collectors.toList());
                    qMap.put("reponsesPossibles", reponsesData);
                }
                
                return qMap;
            }).collect(java.util.stream.Collectors.toList());
            
            return Response.ok(Map.of("questions", questionsData)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erreur lors de la récupération des questions: " + e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/questions/theme/{themeId}")
    public Response getQuestionsByTheme(@PathParam("themeId") Integer themeId) {
        List<Question> questions = questionService.findByTheme(themeId);
        return Response.ok(Map.of("questions", questions)).build();
    }
    
    // Gestion des thèmes
    @GET
    @Path("/themes")
    public Response getAllThemes() {
        List<Theme> themes = questionService.findAllThemes();
        return Response.ok(Map.of("themes", themes)).build();
    }
    
    // Gestion des types de questions
    @GET
    @Path("/types-questions")
    public Response getAllTypesQuestions() {
        List<TypeQuestion> types = questionService.findAllTypesQuestions();
        return Response.ok(Map.of("types", types)).build();
    }
    
    // Gestion des paramètres
    @GET
    @Path("/parametres")
    public Response getAllParametres() {
        List<Parametre> parametres = parametreService.findAll();
        return Response.ok(Map.of("parametres", parametres)).build();
    }
    
    @PUT
    @Path("/parametres/{nom}")
    public Response updateParametre(@PathParam("nom") String nom, Map<String, String> data) {
        try {
            String valeur = data.get("valeur");
            Parametre parametre = parametreService.updateParametre(nom, valeur);
            return Response.ok(Map.of(
                "message", "Paramètre mis à jour avec succès",
                "parametre", parametre
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    // Validation des candidats
    @GET
    @Path("/candidats")
    public Response getAllCandidats() {
        try {
            List<Candidat> candidats = candidatService.findAll();
            
            // Créer une liste de candidats simplifiés pour éviter les problèmes de sérialisation
            List<Map<String, Object>> candidatsData = candidats.stream().map(c -> {
                Map<String, Object> cMap = new java.util.HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("nom", c.getNom());
                cMap.put("prenom", c.getPrenom());
                cMap.put("email", c.getEmail());
                cMap.put("gsm", c.getGsm());
                cMap.put("ecole", c.getEcole());
                cMap.put("filiere", c.getFiliere());
                cMap.put("estValide", c.getEstValide());
                cMap.put("codeSession", c.getCodeSession());
                if (c.getCreatedAt() != null) {
                    cMap.put("createdAt", c.getCreatedAt().toString());
                }
                return cMap;
            }).collect(java.util.stream.Collectors.toList());
            
            return Response.ok(Map.of("candidats", candidatsData)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erreur lors de la récupération des candidats"))
                .build();
        }
    }
    
    @GET
    @Path("/candidats/en-attente")
    public Response getCandidatsEnAttente() {
        List<Candidat> candidats = candidatService.findByEstValide(false);
        return Response.ok(Map.of("candidats", candidats)).build();
    }
    
    @POST
    @Path("/candidats/{id}/valider")
    public Response validerCandidat(@PathParam("id") Integer id) {
        try {
            Candidat candidat = candidatService.validerInscription(id);
            
            // Créer un objet simplifié pour éviter les problèmes de sérialisation
            Map<String, Object> candidatMap = new java.util.HashMap<>();
            candidatMap.put("id", candidat.getId());
            candidatMap.put("nom", candidat.getNom());
            candidatMap.put("prenom", candidat.getPrenom());
            candidatMap.put("email", candidat.getEmail());
            candidatMap.put("gsm", candidat.getGsm());
            candidatMap.put("ecole", candidat.getEcole());
            candidatMap.put("filiere", candidat.getFiliere());
            candidatMap.put("estValide", candidat.getEstValide());
            candidatMap.put("codeSession", candidat.getCodeSession());
            if (candidat.getCreatedAt() != null) {
                candidatMap.put("createdAt", candidat.getCreatedAt().toString());
            }
            
            return Response.ok(Map.of(
                "message", "Candidat validé avec succès",
                "candidat", candidatMap
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/candidats/{id}/rejeter")
    public Response rejeterCandidat(@PathParam("id") Integer id) {
        try {
            candidatService.deleteCandidat(id);
            return Response.ok(Map.of("message", "Candidat rejeté avec succès")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/candidats/{id}/envoyer-code")
    public Response envoyerCodeCandidat(@PathParam("id") Integer id) {
        try {
            Optional<Candidat> candidatOpt = candidatService.findById(id);
            if (candidatOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Candidat non trouvé"))
                    .build();
            }
            
            Candidat candidat = candidatOpt.get();
            if (!candidat.getEstValide() || candidat.getCodeSession() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Le candidat n'est pas encore validé"))
                    .build();
            }
            
            emailService.envoyerEmailValidation(candidat);
            return Response.ok(Map.of("message", "Code de session envoyé par email")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    // Gestion des créneaux
    @POST
    @Path("/creneaux")
    public Response createCreneau(CreneauHoraire creneau) {
        try {
            CreneauHoraire created = creneauHoraireService.createCreneau(creneau);
            
            // Créer un objet simplifié pour éviter les problèmes de sérialisation
            Map<String, Object> creneauMap = new java.util.HashMap<>();
            creneauMap.put("id", created.getId());
            if (created.getDateExam() != null) {
                creneauMap.put("dateExam", created.getDateExam().toString());
            }
            if (created.getHeureDebut() != null) {
                creneauMap.put("heureDebut", created.getHeureDebut().toString());
            }
            if (created.getHeureFin() != null) {
                creneauMap.put("heureFin", created.getHeureFin().toString());
            }
            creneauMap.put("dureeMinutes", created.getDureeMinutes());
            creneauMap.put("placesDisponibles", created.getPlacesDisponibles());
            creneauMap.put("estComplet", created.getEstComplet());
            if (created.getCreatedAt() != null) {
                creneauMap.put("createdAt", created.getCreatedAt().toString());
            }
            
            return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "message", "Créneau créé avec succès",
                    "creneau", creneauMap
                ))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/creneaux/{id}")
    public Response updateCreneau(@PathParam("id") Integer id, CreneauHoraire creneau) {
        try {
            creneau.setId(id);
            CreneauHoraire updated = creneauHoraireService.updateCreneau(creneau);
            
            // Créer un objet simplifié pour éviter les problèmes de sérialisation
            Map<String, Object> creneauMap = new java.util.HashMap<>();
            creneauMap.put("id", updated.getId());
            if (updated.getDateExam() != null) {
                creneauMap.put("dateExam", updated.getDateExam().toString());
            }
            if (updated.getHeureDebut() != null) {
                creneauMap.put("heureDebut", updated.getHeureDebut().toString());
            }
            if (updated.getHeureFin() != null) {
                creneauMap.put("heureFin", updated.getHeureFin().toString());
            }
            creneauMap.put("dureeMinutes", updated.getDureeMinutes());
            creneauMap.put("placesDisponibles", updated.getPlacesDisponibles());
            creneauMap.put("estComplet", updated.getEstComplet());
            if (updated.getCreatedAt() != null) {
                creneauMap.put("createdAt", updated.getCreatedAt().toString());
            }
            
            return Response.ok(Map.of(
                "message", "Créneau mis à jour avec succès",
                "creneau", creneauMap
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @DELETE
    @Path("/creneaux/{id}")
    public Response deleteCreneau(@PathParam("id") Integer id) {
        try {
            creneauHoraireService.deleteCreneau(id);
            return Response.ok(Map.of("message", "Créneau supprimé avec succès")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/creneaux")
    public Response getAllCreneaux() {
        try {
            List<CreneauHoraire> creneaux = creneauHoraireService.findAll();
            
            // Créer une liste de créneaux simplifiés pour éviter les problèmes de sérialisation
            List<Map<String, Object>> creneauxData = creneaux.stream().map(c -> {
                Map<String, Object> cMap = new java.util.HashMap<>();
                cMap.put("id", c.getId());
                // Formater les dates pour éviter les problèmes de sérialisation
                if (c.getDateExam() != null) {
                    cMap.put("dateExam", c.getDateExam().toString());
                }
                if (c.getHeureDebut() != null) {
                    cMap.put("heureDebut", c.getHeureDebut().toString());
                }
                if (c.getHeureFin() != null) {
                    cMap.put("heureFin", c.getHeureFin().toString());
                }
                cMap.put("dureeMinutes", c.getDureeMinutes());
                cMap.put("placesDisponibles", c.getPlacesDisponibles());
                cMap.put("estComplet", c.getEstComplet());
                if (c.getCreatedAt() != null) {
                    cMap.put("createdAt", c.getCreatedAt().toString());
                }
                return cMap;
            }).collect(java.util.stream.Collectors.toList());
            
            return Response.ok(Map.of("creneaux", creneauxData)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erreur lors de la récupération des créneaux: " + e.getMessage()))
                .build();
        }
    }
    
    // Statistiques
    @GET
    @Path("/stats/overview")
    public Response getStatsOverview() {
        Map<String, Object> stats = Map.of(
            "totalCandidats", candidatService.count(),
            "candidatsValides", candidatService.findByEstValide(true).size(),
            "candidatsEnAttente", candidatService.findByEstValide(false).size(),
            "totalCreneaux", creneauHoraireService.findAll().size(),
            "creneauxDisponibles", creneauHoraireService.countAvailableCreneaux(),
            "totalQuestions", questionService.count(),
            "statsResultats", resultatService.getStatsGlobales()
        );
        
        return Response.ok(stats).build();
    }
    
    @GET
    @Path("/stats/questions")
    public Response getStatsQuestions() {
        Map<String, Object> stats = questionService.getStatsQuestions();
        return Response.ok(stats).build();
    }
    
    @GET
    @Path("/stats/candidats")
    public Response getStatsCandidats() {
        Map<String, Object> stats = Map.of(
            "total", candidatService.count(),
            "valides", candidatService.findByEstValide(true).size(),
            "enAttente", candidatService.findByEstValide(false).size(),
            "recent", candidatService.findRecentCandidates(10)
        );
        
        return Response.ok(stats).build();
    }
    
    // Gestion des administrateurs
    @GET
    @Path("/administrateurs")
    public Response getAllAdministrateurs() {
        List<Administrateur> admins = administrateurRepository.findAll();
        return Response.ok(Map.of("administrateurs", admins)).build();
    }
    
    @POST
    @Path("/administrateurs")
    public Response createAdministrateur(Map<String, String> adminData) {
        try {
            Administrateur admin = new Administrateur();
            admin.setUsername(adminData.get("username"));
            admin.setPassword(BCrypt.hashpw(adminData.get("password"), BCrypt.gensalt()));
            admin.setEmail(adminData.get("email"));
            admin.setNom(adminData.get("nom"));
            admin.setPrenom(adminData.get("prenom"));
            
            Administrateur created = administrateurRepository.create(admin);
            created.setPassword(null); // Ne pas renvoyer le mot de passe
            
            return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "message", "Administrateur créé avec succès",
                    "administrateur", created
                ))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/administrateurs/{id}/password")
    public Response changePassword(@PathParam("id") Integer id, Map<String, String> data) {
        try {
            String oldPassword = data.get("oldPassword");
            String newPassword = data.get("newPassword");
            
            Optional<Administrateur> adminOpt = administrateurRepository.findById(id);
            if (adminOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Administrateur non trouvé"))
                    .build();
            }
            
            Administrateur admin = adminOpt.get();
            if (!BCrypt.checkpw(oldPassword, admin.getPassword())) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Ancien mot de passe incorrect"))
                    .build();
            }
            
            admin.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            administrateurRepository.update(admin);
            
            return Response.ok(Map.of("message", "Mot de passe changé avec succès")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    // Obtenir toutes les sessions de test pour l'administration
    @GET
    @Path("/resultats/sessions")
    public Response getAllSessions() {
        try {
            List<SessionTest> sessions = testService.getAllSessions();
            return Response.ok(sessions).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}
