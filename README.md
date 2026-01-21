# Gestion Tests en Ligne

## Architecture
- **Backend**: Jakarta EE (WildFly)
- **Frontend**: React.js
- **Base de données**: MySQL (XAMPP)
- **Communication**: REST API
## Technologies utilisées

- **React 18** - Framework JavaScript
- **React Router** - Routage client
- **Tailwind CSS** - Framework CSS
- **Lucide React** - Icônes
- **React Hook Form** - Gestion des formulaires
## Fonctionnalités

### Pour les candidats
- **Inscription** : Création de compte avec choix de créneau horaire
- **Connexion** : Accès via code de session envoye par email au candidat
- **Passation de test** : Interface chronométrée avec navigation entre questions
- **Résultats** : Consultation des performances avec un score envoye par email au candidat

### Pour les administrateurs
- **Tableau de bord** : Vue d'ensemble avec statistiques
- **Gestion des candidats** : Validation, rejet, envoi de codes
- **Gestion des tests** : Création et modification de questions
- **Gestion des résultats** : Consultation et export des résultats
- **Gestion des créneaux** : Planification des sessions de test creation des creneaux horaires

### Styles
L'application utilise Tailwind CSS avec des composants personnalisés définis dans `index.css`.

### Composants
- Les composants sont organisés par fonctionnalité
- Les hooks personnalisés sont dans le dossier `hooks`
- Les utilitaires sont dans le dossier `utils`

### Routage
Le routage est géré par React Router avec des routes protégées pour l'administration.
##  Vidéo de démonstration de l'application

 **[Voir la vidéo de démonstration](https://drive.google.com/file/d/1BR880qlBrqPq7p6CqqkOTLlYi2PgEE03/view?usp=drive_link)**

Cette vidéo présente :
- le fonctionnement général de l'application
- les principales fonctionnalités
- l’interface utilisateur



