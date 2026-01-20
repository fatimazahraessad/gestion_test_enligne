# Guide de Dépannage - Affichage des Candidats

## 🚨 **Problème** : Les candidats inscrits ne s'affichent pas dans l'administration

## 🔍 **Diagnostic**

### 1. **Vérification des URLs**
Le frontend utilise maintenant les URLs complètes :
- ✅ `http://localhost:8080/gestion-tests-backend/api/admin/candidats`
- ❌ `/api/admin/candidats` (ancienne version)

### 2. **Endpoints backend ajoutés**
```java
// Récupérer tous les candidats
GET /api/admin/candidats

// Récupérer candidats en attente
GET /api/admin/candidats/en-attente

// Valider un candidat
POST /api/admin/candidats/{id}/valider

// Rejeter un candidat
POST /api/admin/candidats/{id}/rejeter

// Envoyer le code par email
POST /api/admin/candidats/{id}/envoyer-code
```

## 🛠️ **Solutions**

### Solution 1 : Redéployer l'application
Les modifications backend nécessitent un redéploiement :

1. **Compiler le backend** :
   ```bash
   cd backend
   mvn clean package
   ```

2. **Déployer sur WildFly** :
   - Via la console d'administration : http://localhost:8080/console
   - Ou copier le WAR dans le répertoire de déploiement

3. **Redémarrer WildFly** si nécessaire

### Solution 2 : Vérifier la connexion API
Testez l'endpoint directement :

```bash
# Test avec PowerShell
Invoke-WebRequest -Uri "http://localhost:8080/gestion-tests-backend/api/admin/candidats" -Method GET -UseBasicParsing

# Test avec curl
curl -X GET http://localhost:8080/gestion-tests-backend/api/admin/candidats
```

**Résultat attendu** : Status 200 avec liste des candidats

### Solution 3 : Vérifier la base de données
Vérifiez que des candidats existent :

```sql
-- Vérifier les candidats
SELECT * FROM candidats;

-- Vérifier les inscriptions
SELECT * FROM inscriptions;
```

### Solution 4 : Vérifier les logs WildFly
Consultez les logs pour d'éventuelles erreurs :
- Console WildFly : http://localhost:8080/console
- Fichiers de logs : `wildfly/standalone/log/server.log`

## 🧪 **Tests de validation**

### Test 1 : Interface d'administration
1. Accéder à : `http://localhost:3000/admin`
2. Cliquer sur "Candidats" dans le menu
3. Vérifier que la liste se charge

### Test 2 : Créer un nouveau candidat
1. Accéder à : `http://localhost:3000/inscription`
2. Remplir le formulaire
3. Soumettre l'inscription
4. Retourner à l'admin pour vérifier l'affichage

### Test 3 : Validation d'un candidat
1. Sélectionner un candidat "En attente"
2. Cliquer sur l'icône de validation (✓)
3. Vérifier que le statut change

## 🔧 **Modifications apportées**

### Frontend (`CandidatesManagement.js`)
- URLs corrigées avec le chemin complet
- Gestion du format de réponse (`data.candidats || data`)
- Actions de validation/rejet/envoi de code

### Backend (`AdministrationResource.java`)
- Ajout de l'endpoint `GET /candidats`
- Ajout des endpoints de validation/rejet/envoi
- Gestion des erreurs appropriée

## 📋 **Checklist de résolution**

- [ ] Backend recompilé et redéployé
- [ ] URLs frontend corrigées
- [ ] Base de données contient des candidats
- [ ] API répond correctement (Status 200)
- [ ] Interface admin affiche les candidats
- [ ] Actions de validation fonctionnelles

## 🚀 **Prochaines étapes**

1. **Redéployer l'application** avec les nouvelles modifications
2. **Tester l'interface** d'administration
3. **Valider le processus** complet d'inscription → validation → code session
4. **Documenter** les procédures pour les utilisateurs

---

**Note** : Le problème principal vient du fait que les modifications backend ne sont pas encore déployées sur WildFly. Un redéploiement complet est nécessaire.
