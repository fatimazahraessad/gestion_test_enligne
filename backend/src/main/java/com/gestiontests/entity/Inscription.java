package com.gestiontests.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_candidat", "id_creneau"}, name = "unique_inscription"))
public class Inscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @NotNull(message = "Le candidat est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidat", nullable = false, foreignKey = @ForeignKey(name = "fk_inscription_candidat"))
    private Candidat candidat;
    
    @NotNull(message = "Le créneau est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_creneau", nullable = false, foreignKey = @ForeignKey(name = "fk_inscription_creneau"))
    private CreneauHoraire creneau;
    
    @Column(name = "date_inscription", updatable = false)
    private LocalDateTime dateInscription;
    
    @Column(name = "est_confirme", nullable = false)
    private Boolean estConfirme = false;
    
    // Constructeurs
    public Inscription() {}
    
    public Inscription(Candidat candidat, CreneauHoraire creneau) {
        this.candidat = candidat;
        this.creneau = creneau;
        this.dateInscription = LocalDateTime.now();
        this.estConfirme = false;
    }
    
    // Getters et Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Candidat getCandidat() {
        return candidat;
    }
    
    public void setCandidat(Candidat candidat) {
        this.candidat = candidat;
    }
    
    public CreneauHoraire getCreneau() {
        return creneau;
    }
    
    public void setCreneau(CreneauHoraire creneau) {
        this.creneau = creneau;
    }
    
    public LocalDateTime getDateInscription() {
        return dateInscription;
    }
    
    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }
    
    public Boolean getEstConfirme() {
        return estConfirme;
    }
    
    public void setEstConfirme(Boolean estConfirme) {
        this.estConfirme = estConfirme;
    }
    
    @PrePersist
    protected void onCreate() {
        dateInscription = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "Inscription{" +
                "id=" + id +
                ", candidat=" + (candidat != null ? candidat.getNom() + " " + candidat.getPrenom() : "null") +
                ", creneau=" + (creneau != null ? creneau.getDateExam() + " " + creneau.getHeureDebut() : "null") +
                ", dateInscription=" + dateInscription +
                ", estConfirme=" + estConfirme +
                '}';
    }
}
