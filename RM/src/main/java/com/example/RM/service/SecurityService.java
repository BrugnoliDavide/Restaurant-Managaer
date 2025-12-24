package com.example.RM.service;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SecurityService {

    private static final Logger logger =
            Logger.getLogger(SecurityService.class.getName());

    private SecurityService() {
        throw new AssertionError("Classe di utilità non istanziabile");
    }

    /**
     * Autentica un utente confrontando la password inserita con l'hash salvato.
     *
     * @param username          username
     * @param candidatePassword password in chiaro inserita
     * @return ruolo se autenticato, null altrimenti
     */
    public static String authenticate(String username, String candidatePassword) {

        final String sql =
                "SELECT password, role FROM users WHERE username = ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                String storedHash = rs.getString("password");
                String role       = rs.getString("role");

                if (BCrypt.checkpw(candidatePassword, storedHash)) {
                    return role;
                }
            }

        } catch (SQLException e) {
            logger.log(
                    Level.SEVERE,
                    "Errore imprevisto durante il login per utente: " + username,
                    e
            );
        }

        return null;
    }

    /**
     * Registra un nuovo utente salvando SOLO l'hash della password.
     */
    public static boolean registerUser(
            String username,
            String plainPassword,
            String role
    ) {

        final String sql =
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        String hashedPassword =
                BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, role);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.log(
                    Level.SEVERE,
                    "Errore durante la creazione dell'utente: " + username,
                    e
            );
            return false;
        }
    }
}
