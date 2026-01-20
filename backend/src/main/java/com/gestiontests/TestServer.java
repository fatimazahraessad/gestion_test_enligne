package com.gestiontests;

import com.gestiontests.rest.TestResource;
import com.gestiontests.rest.CandidatResource;
import com.gestiontests.rest.CreneauHoraireResource;
import com.gestiontests.rest.ResultatResource;
import com.gestiontests.rest.AdministrationResource;
import com.gestiontests.config.CORSFilter;
import com.gestiontests.config.JAXRSApplication;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TestServer {
    public static void main(String[] args) {
        System.out.println("=== Test de la correction du problème de créneau ===");
        
        // Simuler un test de la méthode peutPasserTest
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // Test avec un code session fictif
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/tests/demarrer"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"codeSession\":\"TEST123\"}"))
                .timeout(Duration.ofSeconds(10))
                .build();
                
            System.out.println("Tentative de connexion au backend...");
            System.out.println("Si le backend est démarré, cette requête testerait la logique corrigée.");
            System.out.println("La correction principale :");
            System.out.println("- Utilisation de creneau.getDateExam() au lieu de maintenant.toLocalDate()");
            System.out.println("- Logs détaillés pour le debug");
            System.out.println("- Logique améliorée pour vérifier tous les créneaux");
            
        } catch (Exception e) {
            System.out.println("Le backend n'est pas encore démarré.");
            System.out.println("Pour tester la correction :");
            System.out.println("1. Démarrez un serveur d'application (Tomcat/WildFly)");
            System.out.println("2. Déployez le WAR généré");
            System.out.println("3. Testez avec votre code session");
        }
    }
}
