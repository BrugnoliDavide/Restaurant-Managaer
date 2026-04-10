package com.example.rm.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.rm.service.DBConstants.POSTGRES_PREFIX;

/**
 * Gestisce la configurazione e l'accesso alla connessione JDBC PostgreSQL.
 *
 * <p>Thread-safety garantita tramite:
 * <ul>
 *   <li>{@code volatile} su {@link ConnectionConfig} per visibilità immediata tra thread;</li>
 *   <li>{@code synchronized} su {@link #configure} per atomicità della scrittura;</li>
 *   <li>lettura singola in variabile locale nei metodi di lettura per evitare race condition.</li>
 * </ul>
 */
public final class ConnectionManager {

    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());

    /**
     * Record immutabile che aggrega le credenziali di connessione.
     *
     * <p>L'aggregazione in un unico oggetto è la chiave della thread-safety:
     * un singolo campo {@code volatile} garantisce che url, user e pass
     * vengano sempre letti come un'unità coerente.</p>
     */
    private record ConnectionConfig(String url, String user, String pass) {}

    private static volatile ConnectionConfig config = null;

    private ConnectionManager() {
        throw new IllegalStateException("Utility class");
    }

    // -------------------------------------------------------------------------
    // Configurazione
    // -------------------------------------------------------------------------

    /**
     * Configura la connessione al database PostgreSQL.
     *
     * <p>{@code synchronized} per evitare che due thread sovrascrivano
     * {@code config} in modo concorrente, producendo uno stato inconsistente.</p>
     *
     * @param ip       indirizzo del server
     * @param port     porta del server
     * @param dbName   nome del database
     * @param username utente di accesso
     * @param password password di accesso
     */
    public static synchronized void configure(String ip, String port,
                                              String dbName, String username,
                                              String password) {
        String url = POSTGRES_PREFIX + ip + ":" + port + "/" + dbName;
        config = new ConnectionConfig(url, username, password);
        OrderService.usePostgres();
        logger.log(Level.INFO, "Configurazione DB aggiornata: {0}", url);
    }

    /**
     * Carica la configurazione dalle preferenze persistenti.
     * Non fa nulla se i campi obbligatori risultano vuoti.
     */
    public static void loadFromPreferences() {
        String host = DBConfigStore.getHost();
        String port = DBConfigStore.getPort();
        String db   = DBConfigStore.getDbName();
        String user = DBConfigStore.getUser();
        String pass = DBConfigStore.getPassword();

        if (!host.isBlank() && !port.isBlank() && !db.isBlank() && !user.isBlank()) {
            configure(host, port, db, user, pass);
        }
    }

    // -------------------------------------------------------------------------
    // Accesso alla connessione
    // -------------------------------------------------------------------------

    /**
     * Apre e restituisce una nuova connessione JDBC.
     *
     * <p>Legge {@code config} una sola volta in variabile locale:
     * una doppia lettura potrebbe leggere due versioni diverse se un altro
     * thread chiama {@link #configure} nel mezzo.</p>
     *
     * @return una nuova {@link Connection} attiva
     * @throws SQLException           se la connessione fallisce
     * @throws IllegalStateException  se {@link #configure} non è stato invocato
     */
    public static Connection getConnection() throws SQLException {
        ConnectionConfig current = config;
        if (current == null) {
            throw new IllegalStateException("Database non configurato");
        }
        return DriverManager.getConnection(current.url(), current.user(), current.pass());
    }

    /**
     * @return {@code true} se {@link #configure} è stato invocato almeno una volta
     */
    public static boolean isConfigured() {
        return config != null;
    }

    /**
     * Verifica che la connessione al database sia effettivamente raggiungibile.
     *
     * @return {@code true} se la connessione riesce, {@code false} altrimenti
     */
    public static boolean testConnection() {
        if (!isConfigured()) {
            logger.warning("Tentativo test DB senza configurazione completa");
            return false;
        }
        try (Connection conn = getConnection()) {
            logger.info("Connessione al DB riuscita");
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Connessione al DB fallita", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Getter per la UI di configurazione
    // Ogni metodo legge config una sola volta per coerenza.
    // -------------------------------------------------------------------------

    public static String getHost() {
        ConnectionConfig current = config;
        if (current == null) return "";
        String noPrefix = current.url().replace(POSTGRES_PREFIX, "");
        return noPrefix.substring(0, noPrefix.indexOf(':'));
    }

    public static String getPort() {
        ConnectionConfig current = config;
        if (current == null) return "";
        String noPrefix = current.url().replace(POSTGRES_PREFIX, "");
        int colonIdx = noPrefix.indexOf(':');
        int slashIdx = noPrefix.indexOf('/');
        return noPrefix.substring(colonIdx + 1, slashIdx);
    }

    public static String getDbName() {
        ConnectionConfig current = config;
        if (current == null) return "";
        return current.url().substring(current.url().lastIndexOf('/') + 1);
    }

    public static String getUser() {
        ConnectionConfig current = config;
        return current != null ? current.user() : "";
    }

    public static boolean hasPassword() {
        ConnectionConfig current = config;
        return current != null && current.pass() != null && !current.pass().isBlank();
    }
}