package com.example.rm.controller;

/**
 * Use case per la gestione della configurazione del database.
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

        public DBConfig(String host, String port, String dbName, String username, boolean hasPassword) {
            this.host = host;
            this.port = port;
            this.dbName = dbName;
            this.username = username;
            this.hasPassword = hasPassword;
        }
    }

    /**
     * Carica la configurazione corrente del database.
     */
    DBConfig loadConfig();

    /**
     * Salva la nuova configurazione del database.
     *
     * @param host Indirizzo del server
     * @param port Porta
     * @param dbName Nome del database
     * @param username Username
     * @param password Password (null o vuota per mantenere quella esistente)
     * @return true se salvato con successo
     */
    boolean saveConfig(String host, String port, String dbName, String username, String password);
}
