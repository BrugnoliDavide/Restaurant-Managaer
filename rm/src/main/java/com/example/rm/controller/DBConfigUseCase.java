package com.example.rm.controller;

/**
 * Use case per la gestione della configurazione del database.
 * Supporta due modalità: parametri separati e URL JDBC diretto.
 */
public interface DBConfigUseCase {

    /**
     * DTO per i dati di configurazione DB.
     */
    class DBConfig {
        public final String host;
        public final String port;
        public final String dbName;
        public final String username;
        public final boolean hasPassword;
        public final String jdbcUrl;

        public DBConfig(String host, String port, String dbName,
                        String username, boolean hasPassword, String jdbcUrl) {
            this.host = host;
            this.port = port;
            this.dbName = dbName;
            this.username = username;
            this.hasPassword = hasPassword;
            this.jdbcUrl = jdbcUrl;
        }
    }

    /**
     * Risultato di un'operazione di salvataggio con test di connessione.
     */
    class SaveResult {
        public final boolean saved;
        public final boolean connectionOk;
        public final String errorMessage;

        private SaveResult(boolean saved, boolean connectionOk, String errorMessage) {
            this.saved = saved;
            this.connectionOk = connectionOk;
            this.errorMessage = errorMessage;
        }

        public static SaveResult success() {
            return new SaveResult(true, true, null);
        }

        public static SaveResult savedButConnectionFailed(String error) {
            return new SaveResult(true, false, error);
        }

        public static SaveResult validationError(String error) {
            return new SaveResult(false, false, error);
        }
    }

    /**
     * Carica la configurazione corrente del database.
     */
    DBConfig loadConfig();

    /**
     * Salva la configurazione tramite parametri separati e testa la connessione.
     *
     * @param host     Indirizzo del server
     * @param port     Porta
     * @param dbName   Nome del database
     * @param username Username
     * @param password Password (null o vuota per mantenere quella esistente)
     * @return risultato dell'operazione con dettagli sull'eventuale errore
     */
    SaveResult saveConfig(String host, String port, String dbName,
                          String username, String password);

    /**
     * Salva la configurazione tramite URL JDBC diretto e testa la connessione.
     *
     * @param jdbcUrl  URL JDBC completo
     * @param username Username
     * @param password Password
     * @return risultato dell'operazione con dettagli sull'eventuale errore
     */
    SaveResult saveConfigWithUrl(String jdbcUrl, String username, String password);
}