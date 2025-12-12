package com.example.demo.service;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class SecurityService {

    private static final String URL = "jdbc:postgresql://localhost:5432/restaurant_db";
    private static final String USER = "admin";
    private static final String PASS = "password123";

    // 1. AUTENTICAZIONE (Login)
    // Ritorna il RUOLO se la password matcha l'hash, altrimenti null.
    public static String authenticate(String username, String candidatePassword) {
        String sql = "SELECT password, role FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                String role = rs.getString("role");

                // LA MAGIA: BCrypt controlla se la password in chiaro corrisponde all'hash
                if (BCrypt.checkpw(candidatePassword, storedHash)) {
                    return role;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Utente non trovato o password errata
    }

    // 2. REGISTRAZIONE UTENTE (Hash della password)
    public static boolean registerUser(String username, String plainPassword, String role) {
        // Genera il Salt e Hasha la password
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // 12 è il "costo" (più è alto, più è sicuro e lento)

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword); // si salva SOLO l'hash
            pstmt.setString(3, role);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Errore creazione utente: " + e.getMessage());
            return false;
        }
    }
}