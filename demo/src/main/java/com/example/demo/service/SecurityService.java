package com.example.demo.service;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.logging.Level;

import static com.example.demo.view.LoginController.logger;

public final class SecurityService {



        private static final String URL = "jdbc:postgresql://localhost:5432/restaurant_db";
        private static final String USER = "admin";
        private static final String PASS = "password123";

    private SecurityService() {
        throw new AssertionError("Classe di utilità non istanziabile");
    }

        public static String authenticate(String username, String candidatePassword) {
            String sql = "SELECT password, role FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String role = rs.getString("role");


                    if (BCrypt.checkpw(candidatePassword, storedHash)) {
                        return role;
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel nel login:", e);
            }
            return null;
        }

        public static boolean registerUser(String username, String plainPassword, String role) {

            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // 12 è il "costo" (più è alto, più è sicuro e lento)

            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword); // si salva SOLO l'hash
                pstmt.setString(3, role);

                return pstmt.executeUpdate() > 0;

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Errore creazione utente: ", e);
                return false;
            }
        }

}