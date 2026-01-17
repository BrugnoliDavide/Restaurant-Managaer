package com.example.rm.service;

import com.example.rm.exception.AuthenticationException;
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
     * Autentica un utente confrontando la password inserita con l'hash salvato per garantire la sicurezza
     *
     * @param username          username
     * @param candidatePassword password in chiaro inserita
     * @return ruolo se autenticato, null altrimenti
     */
    public static String authenticate(String username, String candidatePassword) {

        if (username == null || username.isBlank()
                || candidatePassword == null || candidatePassword.isBlank()) {
            throw new IllegalArgumentException("Username o password non validi");
        }

        final String sql =
                "SELECT password, role FROM users WHERE username = ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (!rs.next()) {
                    throw new AuthenticationException("Credenziali non valide");
                }

                String storedHash = rs.getString("password");
                String role       = rs.getString("role");

                if (!BCrypt.checkpw(candidatePassword, storedHash)) {
                    throw new AuthenticationException("Credenziali non valide");
                }

                return role;
            }

        } catch (SQLException e) {
            logger.log(
                    Level.SEVERE,
                    "Errore durante il login per utente: {0}", username
            );
            throw new AuthenticationException(
                    "Errore di accesso al database durante l'autenticazione", e
            );
        }
    }


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
            logger.log(Level.SEVERE, "Errore durante la creazione di utente:  {0}, {1}", new Object[]{username, e});
            return false;
        }
    }




    public static boolean changePassword(
            String username,
            String currentPassword,
            String newPassword
    ) {
        //VERIFICA PASSWORD CORRENTE
        String currentRole = authenticate(username, currentPassword);

        if (currentRole == null) {
            logger.log(Level.WARNING, "Tentativo cambio password fallito per {0}: password corrente errata", username);
            return false;
        }

        // GENERA HASH DELLA NUOVA PASSWORD
        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));

        // AGGIORNA DATABASE
        final String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newHashedPassword);
            pstmt.setString(2, username);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Password cambiata con successo per utente: {0}", username);
                return true;
            } else {
                logger.log(Level.WARNING, "Nessuna riga aggiornata per utente: {0}", username);
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante cambio password per utente: {0}", username);
            return false;
        }
    }
}
