@echo off
echo ========================================
echo Déploiement de l'application backend
echo ========================================

cd /d "d:\gestion\backend"

echo.
echo 1. Nettoyage et compilation...
call mvn clean package

if %ERRORLEVEL% neq 0 (
    echo Erreur lors de la compilation!
    pause
    exit /b 1
)

echo.
echo 2. Copie du WAR vers WildFly...
REM Adapter ce chemin selon votre installation WildFly
copy "target\gestion-tests-backend.war" "C:\wildfly\standalone\deployments\"

if %ERRORLEVEL% neq 0 (
    echo Erreur lors de la copie du WAR!
    echo Veuillez adapter le chemin de WildFly dans ce script.
    pause
    exit /b 1
)

echo.
echo 3. Vérification du déploiement...
timeout /t 5 /nobreak

echo.
echo ========================================
echo Déploiement terminé!
echo ========================================
echo.
echo L'application sera disponible dans quelques instants:
echo - Backend: http://localhost:8080/gestion-tests-backend
echo - Console Admin: http://localhost:8080/console
echo.
pause
