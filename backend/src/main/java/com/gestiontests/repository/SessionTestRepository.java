package com.gestiontests.repository;

import com.gestiontests.entity.SessionTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Named
@ApplicationScoped
public class SessionTestRepository extends GenericRepository<SessionTest, Integer> {
    
    public SessionTestRepository() {
        super(SessionTest.class);
    }
    
    public Optional<SessionTest> findByCodeSession(String codeSession) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.codeSession = :codeSession", SessionTest.class);
        query.setParameter("codeSession", codeSession);
        List<SessionTest> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public Optional<SessionTest> findByCandidatAndToday(Integer candidatId) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.candidat.id = :candidatId AND FUNCTION('DATE', s.dateDebut) = CURRENT_DATE", 
            SessionTest.class);
        query.setParameter("candidatId", candidatId);
        List<SessionTest> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<SessionTest> findByCandidat(Integer candidatId) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.candidat.id = :candidatId ORDER BY s.dateDebut DESC", 
            SessionTest.class);
        query.setParameter("candidatId", candidatId);
        return query.getResultList();
    }
    
    public List<SessionTest> findByEstTermine(Boolean estTermine) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.estTermine = :estTermine ORDER BY s.dateDebut DESC", 
            SessionTest.class);
        query.setParameter("estTermine", estTermine);
        return query.getResultList();
    }
    
    public List<SessionTest> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.dateDebut BETWEEN :startDate AND :endDate ORDER BY s.dateDebut DESC", 
            SessionTest.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }
    
    public List<SessionTest> findRecentSessions(int limit) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s ORDER BY s.dateDebut DESC", SessionTest.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }
    
    public List<SessionTest> findSessionsByDate(LocalDate date) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE FUNCTION('DATE', s.dateDebut) = :date ORDER BY s.dateDebut DESC", 
            SessionTest.class);
        query.setParameter("date", date);
        return query.getResultList();
    }
    
    public List<SessionTest> findSessionsByScoreRange(Double minScore, Double maxScore) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.pourcentage BETWEEN :minScore AND :maxScore ORDER BY s.pourcentage DESC", 
            SessionTest.class);
        query.setParameter("minScore", minScore);
        query.setParameter("maxScore", maxScore);
        return query.getResultList();
    }
    
    public long countByCandidat(Integer candidatId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(s) FROM SessionTest s WHERE s.candidat.id = :candidatId", Long.class);
        query.setParameter("candidatId", candidatId);
        return query.getSingleResult();
    }
    
    public long countByEstTermine(Boolean estTermine) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(s) FROM SessionTest s WHERE s.estTermine = :estTermine", Long.class);
        query.setParameter("estTermine", estTermine);
        return query.getSingleResult();
    }
    
    public boolean existsByCodeSession(String codeSession) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(s) FROM SessionTest s WHERE s.codeSession = :codeSession", Long.class);
        query.setParameter("codeSession", codeSession);
        return query.getSingleResult() > 0;
    }
    
    public Double getAverageScore() {
        TypedQuery<Double> query = entityManager.createQuery(
            "SELECT AVG(s.pourcentage) FROM SessionTest s WHERE s.estTermine = true", Double.class);
        return query.getSingleResult();
    }
    
    public Double getAverageScoreByDate(LocalDate date) {
        TypedQuery<Double> query = entityManager.createQuery(
            "SELECT AVG(s.pourcentage) FROM SessionTest s WHERE s.estTermine = true AND FUNCTION('DATE', s.dateDebut) = :date", 
            Double.class);
        query.setParameter("date", date);
        return query.getSingleResult();
    }
    
    public List<SessionTest> findTopScorers(int limit) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.estTermine = true ORDER BY s.pourcentage DESC", 
            SessionTest.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }
    
    public List<SessionTest> findByEcole(String ecole) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.candidat.ecole = :ecole ORDER BY s.dateDebut DESC", 
            SessionTest.class);
        query.setParameter("ecole", ecole);
        return query.getResultList();
    }
    
    public List<SessionTest> findByFiliere(String filiere) {
        TypedQuery<SessionTest> query = entityManager.createQuery(
            "SELECT s FROM SessionTest s WHERE s.candidat.filiere = :filiere ORDER BY s.dateDebut DESC", 
            SessionTest.class);
        query.setParameter("filiere", filiere);
        return query.getResultList();
    }
}
