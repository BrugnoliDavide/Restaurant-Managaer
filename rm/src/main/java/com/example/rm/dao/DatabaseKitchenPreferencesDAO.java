package com.example.rm.dao;

import com.example.rm.preference.DemoModeManager;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.preference.PreferencesConstants;
import com.example.rm.preference.PreferencesSerializer;

import java.sql.*;
import java.util.HashSet;
import java.util.logging.Level;

import static com.example.rm.view.KitchenController.logger;

public class DatabaseKitchenPreferencesDAO implements KitchenPreferencesDAO {

    @Override
    public KitchenPreferences loadByUsername(String username) {
        return getKitchenPreferences(username);
    }

    @Override
    public boolean save(KitchenPreferences preferences) {
        if (DemoModeManager.isDemoMode()) {
            logger.log(Level.INFO,"modalità DEMO attiva: preferenze cucina saranno valide fino alla chiusura del sistema");
            return true;
        } else {
            return saveKitchenPreferences(preferences);
        }
    }

    @Override
    public boolean deleteByUsername(String username) {
        return deleteKitchenPreferences(username);
    }


    /**
     * Carica le preferenze di un utente cucina dalla tabella users.
     * Se non esistono, ritorna preferenze di default.
     * @param username Username della cucina
     * @return KitchenPreferences dell'utente
     */
    public static KitchenPreferences getKitchenPreferences(String username) {
        String sql = "SELECT kitchen_preferences FROM users WHERE username = ?";

        try (PreparedStatement pstmt =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String preferencesStr = rs.getString("kitchen_preferences");
                    return PreferencesSerializer.deserialize(preferencesStr, username);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento preferenze cucina per {0}, {1}", new Object[]{ username, e});
        }

        // Fallback: preferenze di default
        return new KitchenPreferences(username,
                PreferencesConstants.DEFAULT_SPLIT_ORDERS,
                new HashSet<>(),
                PreferencesConstants.DEFAULT_INCLUDE_OTHER);
    }

    /**
     * Salva le preferenze di un utente cucina nella colonna kitchen_preferences della tabella users.
     * @param preferences Preferenze da salvare
     * @return true se salvataggio riuscito
     */
    public static boolean saveKitchenPreferences(KitchenPreferences preferences) {
        if (preferences == null || preferences.getUsername() == null) {
            logger.warning("Impossibile salvare preferenze: username mancante");
            return false;
        }

        String sql = "UPDATE users SET kitchen_preferences = ? WHERE username = ?";

        String serialized = PreferencesSerializer.serialize(preferences);

        try (PreparedStatement pstmt =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            if (serialized != null) {
                pstmt.setString(1, serialized);
            } else {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            }

            pstmt.setString(2, preferences.getUsername());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.log(Level.INFO, "Preferenze cucina salvate per {0}", preferences.getUsername());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore salvataggio preferenze cucina", e);
        }

        return false;
    }

    /**
     * Elimina le preferenze di un utente cucina (cioè le setta a NULL).
     * @param username Username della cucina
     * @return true se eliminazione riuscita
     */
    public static boolean deleteKitchenPreferences(String username) {
        String sql = "UPDATE users SET kitchen_preferences = NULL WHERE username = ?";

        try (PreparedStatement pstmt =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.log(Level.INFO, "Preferenze cucina eliminate per {0}", username);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore eliminazione preferenze cucina", e);
        }

        return false;
    }




}
