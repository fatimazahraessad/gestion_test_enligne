-- Base de données pour l'application de gestion des tests en ligne

CREATE DATABASE IF NOT EXISTS gestion_tests;
USE gestion_tests;


CREATE TABLE themes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE types_question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);


CREATE TABLE questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_theme INT NOT NULL,
    id_type_question INT NOT NULL,
    libelle TEXT NOT NULL,
    explication TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_theme) REFERENCES themes(id),
    FOREIGN KEY (id_type_question) REFERENCES types_question(id)
);


CREATE TABLE reponses_possibles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_question INT NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    est_correct BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_question) REFERENCES questions(id) ON DELETE CASCADE
);


CREATE TABLE candidats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    ecole VARCHAR(100) NOT NULL,
    filiere VARCHAR(100),
    email VARCHAR(150) NOT NULL UNIQUE,
    gsm VARCHAR(20) NOT NULL,
    code_session VARCHAR(10) UNIQUE,
    est_valide BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE creneaux_horaires (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date_exam DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    duree_minutes INT NOT NULL,
    places_disponibles INT NOT NULL DEFAULT 1,
    est_complet BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE inscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidat INT NOT NULL,
    id_creneau INT NOT NULL,
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    est_confirme BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_candidat) REFERENCES candidats(id),
    FOREIGN KEY (id_creneau) REFERENCES creneaux_horaires(id),
    UNIQUE KEY unique_inscription (id_candidat, id_creneau)
);


CREATE TABLE sessions_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_candidat INT NOT NULL,
    id_creneau INT NOT NULL,
    code_session VARCHAR(10) NOT NULL UNIQUE,
    date_debut TIMESTAMP,
    date_fin TIMESTAMP,
    est_termine BOOLEAN DEFAULT FALSE,
    score_total INT DEFAULT 0,
    score_max INT DEFAULT 0,
    pourcentage DECIMAL(5,2) DEFAULT 0,
    FOREIGN KEY (id_candidat) REFERENCES candidats(id),
    FOREIGN KEY (id_creneau) REFERENCES creneaux_horaires(id)
);


CREATE TABLE session_questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_session INT NOT NULL,
    id_question INT NOT NULL,
    ordre_affichage INT NOT NULL,
    temps_alloue INT DEFAULT 120, 
    FOREIGN KEY (id_session) REFERENCES sessions_test(id) ON DELETE CASCADE,
    FOREIGN KEY (id_question) REFERENCES questions(id)
);


CREATE TABLE reponses_candidat (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_session_question INT NOT NULL,
    id_reponse_possible INT,
    reponse_text VARCHAR(255),
    temps_reponse INT, 
    date_reponse TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    est_correct BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_session_question) REFERENCES session_questions(id),
    FOREIGN KEY (id_reponse_possible) REFERENCES reponses_possibles(id)
);


CREATE TABLE parametres (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom_param VARCHAR(100) NOT NULL UNIQUE,
    valeur VARCHAR(255) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE administrateurs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL,
    nom VARCHAR(100),
    prenom VARCHAR(100),
    est_actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


  
