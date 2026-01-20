@echo off
echo ========================================
echo Chargement complet de la base de données
echo ========================================

echo.
echo 1. Création de la base de données...
mysql -u root -ppro0809 < schema.sql

echo.
echo 2. Chargement des données de base...
mysql -u root -ppro0809 gestion_tests < data.sql

echo.
echo 3. Ajout des questions complètes...
mysql -u root -ppro0809 gestion_tests < questions_completes.sql

echo.
echo ========================================
echo Base de données chargée avec succès !
echo ========================================
echo.
echo Statistiques :
mysql -u root -ppro0809 gestion_tests -e "
SELECT 
  'Thèmes' as Table, COUNT(*) as Nombre FROM themes
UNION ALL
SELECT 
  'Types de questions', COUNT(*) FROM types_question
UNION ALL
SELECT 
  'Questions', COUNT(*) FROM questions
UNION ALL
SELECT 
  'Réponses possibles', COUNT(*) FROM reponses_possibles
UNION ALL
SELECT 
  'Créneaux horaires', COUNT(*) FROM creneaux_horaires
UNION ALL
SELECT 
  'Administrateurs', COUNT(*) FROM administrateurs;
"

echo.
echo Appuyez sur une touche pour quitter...
pause
