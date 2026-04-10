package com.example.rm.dao;

import com.example.rm.app.UsersFactory;
import com.example.rm.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUserDAO implements UserDAO {

    private static final Logger logger = Logger.getLogger(DatabaseUserDAO.class.getName());

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT username, role FROM users ORDER BY role, username";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User u = UsersFactory.createUser(
                        rs.getString("username"), rs.getString("role"));
                if (u != null) list.add(u);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero utenti", e);
        }
        return list;
    }

    @Override
    public boolean delete(String username) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                logger.log(Level.INFO, "Utente eliminato: {0}", username);
                return true;
            } else {
                logger.log(Level.WARNING, "Utente non trovato: {0}", username);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore eliminazione utente", e);
            return false;
        }
    }

    @Override
    public boolean insert(String username, String hashedPassword, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore creazione utente: {0}", username);
            return false;
        }
    }

    @Override
    public UserCredentials findCredentials(String username) {
        String sql = "SELECT password, role FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserCredentials(
                            rs.getString("password"),
                            rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero credenziali per: {0}", username);
        }
        return null;
    }

    @Override
    public boolean updatePassword(String username, String newHashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newHashedPassword);
            ps.setString(2, username);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                logger.log(Level.INFO, "Password aggiornata per: {0}", username);
                return true;
            } else {
                logger.log(Level.WARNING, "Nessuna riga aggiornata per: {0}", username);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore aggiornamento password", e);
            return false;
        }
    }
}