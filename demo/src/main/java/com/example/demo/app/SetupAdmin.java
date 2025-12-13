/*package com.example.demo.app;

import com.example.demo.service.DatabaseService;
import com.example.demo.service.SecurityService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SetupAdmin {

    // Credenziali DB (Devono essere identiche a quelle nel DatabaseService)
    private static final String URL = "jdbc:postgresql://localhost:5432/restaurant_db";
    private static final String USER = "admin";
    private static final String PASS = "password123";

    public static void main(String[] args) {
        System.out.println("--- INIZIO SETUP ADMIN ---");

        // 1. CREAZIONE AUTOMATICA TABELLA
        // Se la tabella non c'è, la crea Java. Questo risolve il tuo errore.
        createTableIfNotExists();

        String username = "admin";
        String passwordInChiaro = "admin123";
        String ruolo = "manager";

        // 2. Pulizia (ora sicura perché la tabella esiste di sicuro)
        System.out.println("Pulizia utente esistente...");
        DatabaseService.deleteUser(username);

        // 3. Creazione Admin
        System.out.println("Creazione utente admin...");
        boolean successo = SecurityService.registerUser(username, passwordInChiaro, ruolo);

        if (successo) {
            System.out.println("\n✅ SUPER ADMIN CREATO CON SUCCESSO!");
            System.out.println("Username: " + username);
            System.out.println("Password: " + passwordInChiaro);
            System.out.println("Puoi avviare l'app e fare login.");
        } else {
            System.err.println("\n❌ Errore nella creazione dell'admin (Controlla se Docker è acceso).");
        }
    }

    private static void createTableIfNotExists() {
        // SQL per creare la tabella se manca
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "username VARCHAR(50) PRIMARY KEY, " +
                "password VARCHAR(255) NOT NULL, " + // 255 caratteri per stare larghi con l'hash
                "role VARCHAR(20) NOT NULL" +
                ");";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("✅ Tabella 'users' verificata/creata correttamente nel DB.");

        } catch (Exception e) {
            System.err.println("❌ IMPOSSIBILE CREARE LA TABELLA 'USERS':");
            System.err.println("Assicurati che il database 'restaurant_db' esista.");
            e.printStackTrace();
            System.exit(1); // Ferma tutto se non riesce a creare la tabella
        }
    }
}

*/
