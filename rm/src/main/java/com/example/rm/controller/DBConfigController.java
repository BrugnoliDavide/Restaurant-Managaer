package com.example.rm.controller;

import com.example.rm.service.ConnectionManager;
import com.example.rm.service.DBConfigStore;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConfigController implements DBConfigUseCase {

    private static final Logger logger = Logger.getLogger(DBConfigController.class.getName());

    @Override
    public DBConfig loadConfig() {
        String host       = ConnectionManager.getHost();
        String port       = ConnectionManager.getPort();
        String dbName     = ConnectionManager.getDbName();
        String user       = ConnectionManager.getUser();
        boolean hasPass   = ConnectionManager.hasPassword();
        String jdbcUrl    = ConnectionManager.getJdbcUrl();

        return new DBConfig(host, port, dbName, user, hasPass, jdbcUrl);
    }

    @Override
    public SaveResult saveConfig(String host, String port, String dbName,
                                 String username, String password) {
        // Validazioni base
        if (isBlank(host)) {
            return SaveResult.validationError("L'indirizzo del server è obbligatorio.");
        }
        if (isBlank(port)) {
            return SaveResult.validationError("La porta è obbligatoria.");
        }
        if (isBlank(dbName)) {
            return SaveResult.validationError("Il nome del database è obbligatorio.");
        }
        if (isBlank(username)) {
            return SaveResult.validationError("Lo username è obbligatorio.");
        }

        // Risolvi password
        String finalPassword = resolvePassword(password);
        if (finalPassword == null) {
            return SaveResult.validationError(
                    "La password è obbligatoria. Inserire una password valida.");
        }

        // Salva nelle preferenze cifrate
        DBConfigStore.save(
                host.trim(), port.trim(), dbName.trim(),
                username.trim(), finalPassword
        );

        // Configura ConnectionManager
        ConnectionManager.configure(
                host.trim(), port.trim(), dbName.trim(),
                username.trim(), finalPassword
        );

        // Testa la connessione
        return testAndReturnResult();
    }

    @Override
    public SaveResult saveConfigWithUrl(String jdbcUrl, String username, String password) {
        if (isBlank(jdbcUrl)) {
            return SaveResult.validationError("L'URL JDBC è obbligatorio.");
        }
        if (isBlank(username)) {
            return SaveResult.validationError("Lo username è obbligatorio.");
        }

        String finalPassword = resolvePassword(password);
        if (finalPassword == null) {
            return SaveResult.validationError(
                    "La password è obbligatoria. Inserire una password valida.");
        }

        try {
            // Configura con URL diretto
            ConnectionManager.configureWithUrl(
                    jdbcUrl.trim(), username.trim(), finalPassword
            );

            // Salva i parametri estratti nelle preferenze
            DBConfigStore.save(
                    ConnectionManager.getHost(),
                    ConnectionManager.getPort(),
                    ConnectionManager.getDbName(),
                    username.trim(),
                    finalPassword
            );

            return testAndReturnResult();

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "URL JDBC non valido: {0}", e.getMessage());
            return SaveResult.validationError(
                    "Formato URL non valido. Formati accettati:\n"
                            + "• jdbc:postgresql://host:porta/database\n"
                            + "• postgresql://host:porta/database\n"
                            + "• host:porta/database"
            );
        }
    }

    /**
     * Risolvi la password: se vuota, tenta di recuperare quella salvata.
     *
     * @return password valida oppure {@code null} se non disponibile
     */
    private String resolvePassword(String password) {
        if (password != null && !password.isBlank()) {
            return password.trim();
        }
        // Tenta di recuperare la password esistente
        String stored = DBConfigStore.getPassword();
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        return null;
    }

    /**
     * Testa la connessione e restituisce il risultato appropriato.
     */
    private SaveResult testAndReturnResult() {
        boolean connectionOk = ConnectionManager.testConnection();
        if (connectionOk) {
            return SaveResult.success();
        } else {
            String error = ConnectionManager.getLastConnectionError();
            return SaveResult.savedButConnectionFailed(
                    error != null ? error : "Connessione fallita per motivo sconosciuto."
            );
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}