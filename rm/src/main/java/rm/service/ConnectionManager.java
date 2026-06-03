package rm.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rm.service.DBConstants.POSTGRES_PREFIX;

/**
 * Gestisce la configurazione e l'accesso alla connessione JDBC PostgreSQL.
 *
 * <p>Thread-safety garantita tramite:
 * <ul>
 *   <li>{@code volatile} su {@link ConnectionConfig} per visibilità immediata tra thread;</li>
 *   <li>{@code synchronized} su {@link #configure} per atomicità della scrittura;</li>
 *   <li>lettura singola in variabile locale nei metodi di lettura per evitare race condition.</li>
 * </ul>
 *
 * <p>Supporta due modalità di configurazione:
 * <ol>
 *   <li>Parametri separati (host, port, dbName, username, password)</li>
 *   <li>URL JDBC diretto (es. {@code jdbc:postgresql://host:port/dbName})</li>
 * </ol>
 */
public final class ConnectionManager {

    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());

    /**
     * Record immutabile che aggrega le credenziali di connessione.
     */
    private record ConnectionConfig(String url, String user, String pass) {}

    private static final AtomicReference<ConnectionConfig> config =
            new AtomicReference<>(null);

    /** Ultimo errore di connessione, utile per il feedback all'utente. */
    private static final AtomicReference<String> lastConnectionError =
            new AtomicReference<>(null);

    private ConnectionManager() {
        throw new IllegalStateException("Utility class");
    }

    // Configurazione

    /**
     * Configura la connessione al database PostgreSQL tramite parametri separati.
     */
    public static synchronized void configure(String ip, String port,
                                              String dbName, String username,
                                              String password) {
        String url = POSTGRES_PREFIX + ip + ":" + port + "/" + dbName;
        config.set(new ConnectionConfig(url, username, password));
        lastConnectionError.set(null);
        OrderService.usePostgres();
        logger.log(Level.INFO, "Configurazione DB aggiornata: {0}", url);
    }

    /**
     * Configura la connessione al database PostgreSQL tramite URL JDBC diretto.
     *
     * <p>L'URL deve essere nel formato {@code jdbc:postgresql://host:port/dbName}
     * oppure un URL completo con parametri aggiuntivi.</p>
     *
     * @param jdbcUrl  URL JDBC completo
     * @param username utente di accesso
     * @param password password di accesso
     * @throws IllegalArgumentException se l'URL non è valido
     */
    public static synchronized void configureWithUrl(String jdbcUrl, String username,
                                                     String password) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("L'URL JDBC non può essere vuoto");
        }
        String normalizedUrl = normalizeJdbcUrl(jdbcUrl.trim());
        config.set(new ConnectionConfig(normalizedUrl, username, password));
        lastConnectionError.set(null);
        OrderService.usePostgres();
        logger.log(Level.INFO, "Configurazione DB aggiornata tramite URL diretto: {0}",
                normalizedUrl);
    }

    /**
     * Normalizza un URL JDBC, aggiungendo il prefisso se necessario.
     *
     * <p>Accetta i seguenti formati:
     * <ul>
     *   <li>{@code jdbc:postgresql://host:port/db}</li>
     *   <li>{@code postgresql://host:port/db}</li>
     *   <li>{@code host:port/db}</li>
     * </ul>
     */
    static String normalizeJdbcUrl(String input) {
        if (input.startsWith("jdbc:postgresql://")) {
            return input;
        }
        if (input.startsWith("postgresql://")) {
            return "jdbc:" + input;
        }
        // Formato minimo: host:port/db
        if (input.contains(":") && input.contains("/")) {
            return POSTGRES_PREFIX + input;
        }
        throw new IllegalArgumentException(
                "Formato URL non riconosciuto. Formati accettati: " +
                        "jdbc:postgresql://host:port/db, postgresql://host:port/db, host:port/db"
        );
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

        // Verifica anche la password: senza password la connessione fallirà comunque
        if (!host.isBlank() && !port.isBlank() && !db.isBlank()
                && !user.isBlank() && !pass.isBlank()) {
            configure(host, port, db, user, pass);
        } else if (!host.isBlank()) {
            // Log informativo se la configurazione è parziale
            logger.warning("Configurazione DB parziale: password mancante o vuota. "
                    + "Reinserire le credenziali dal pannello di configurazione.");
        }
    }

    // -------------------------------------------------------------------------
    // Accesso alla connessione
    // -------------------------------------------------------------------------

    /**
     * Apre e restituisce una nuova connessione JDBC.
     */
    public static Connection getConnection() throws SQLException {
        ConnectionConfig current = config.get();
        if (current == null) {
            throw new IllegalStateException("Database non configurato");
        }
        return DriverManager.getConnection(current.url(), current.user(), current.pass());
    }

    public static boolean isConfigured() {
        return config.get() != null;
    }

    /**
     * Verifica che la connessione al database sia effettivamente raggiungibile.
     * Salva l'eventuale errore per il feedback all'utente.
     *
     * @return {@code true} se la connessione riesce, {@code false} altrimenti
     */
    public static boolean testConnection() {
        if (!isConfigured()) {
            lastConnectionError.set("Database non configurato. Inserire le credenziali.");
            logger.warning("Tentativo test DB senza configurazione completa");
            return false;
        }
        try (Connection conn = getConnection()) {
            lastConnectionError.set(null);
            logger.info("Connessione al DB riuscita");
            return true;
        } catch (SQLException e) {
            lastConnectionError.set(formatSqlError(e));
            logger.log(Level.WARNING, "Connessione al DB fallita: {0}",
                    lastConnectionError.get());
            return false;
        }
    }
    /**
     * Restituisce l'ultimo errore di connessione in formato leggibile.
     *
     * @return messaggio di errore, oppure {@code null} se l'ultima connessione è riuscita
     */
    public static String getLastConnectionError() {
        return lastConnectionError.get();
    }

    /**
     * Formatta un errore SQL in un messaggio comprensibile per l'utente.
     */
    private static String formatSqlError(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return "Errore sconosciuto";

        // Errori comuni PostgreSQL tradotti in messaggi comprensibili
        if (msg.contains("Connection refused")) {
            return "Connessione rifiutata. Verificare che il server PostgreSQL "
                    + "sia avviato e raggiungibile all'indirizzo configurato.";
        }
        if (msg.contains("password authentication failed")) {
            return "Autenticazione fallita. Username o password errati.";
        }
        if (msg.contains("database") && msg.contains("does not exist")) {
            return "Il database specificato non esiste sul server.";
        }
        if (msg.contains("UnknownHostException") || msg.contains("No such host")) {
            return "Host non trovato. Verificare l'indirizzo del server.";
        }
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return "Timeout di connessione. Il server potrebbe non essere raggiungibile.";
        }
        // Fallback: messaggio originale (troncato se troppo lungo)
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }

    // -------------------------------------------------------------------------
    // Getter per la UI di configurazione
    // -------------------------------------------------------------------------

    public static String getHost() {
        ConnectionConfig current = config.get();
        if (current == null) return "";
        try {
            String noPrefix = current.url().replace(POSTGRES_PREFIX, "");
            int colonIdx = noPrefix.indexOf(':');
            return colonIdx > 0 ? noPrefix.substring(0, colonIdx) : noPrefix;
        } catch (Exception e) {
            return "";
        }
    }

    public static String getPort() {
        ConnectionConfig current = config.get();
        if (current == null) return "";
        try {
            String noPrefix = current.url().replace(POSTGRES_PREFIX, "");
            int colonIdx = noPrefix.indexOf(':');
            int slashIdx = noPrefix.indexOf('/');
            if (colonIdx >= 0 && slashIdx > colonIdx) {
                return noPrefix.substring(colonIdx + 1, slashIdx);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getDbName() {
        ConnectionConfig current = config.get();
        if (current == null) return "";
        try {
            String url = current.url();
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                // Rimuovi eventuali parametri query (?param=value)
                String dbPart = url.substring(lastSlash + 1);
                int queryIdx = dbPart.indexOf('?');
                return queryIdx > 0 ? dbPart.substring(0, queryIdx) : dbPart;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getUser() {
        ConnectionConfig current = config.get();
        return current != null ? current.user() : "";
    }

    public static boolean hasPassword() {
        ConnectionConfig current = config.get();
        return current != null && current.pass() != null && !current.pass().isBlank();
    }

    /**
     * Restituisce l'URL JDBC corrente, utile per la modalità URL diretto.
     *
     * @return URL JDBC configurato, oppure stringa vuota se non configurato
     */
    public static String getJdbcUrl() {
        ConnectionConfig current = config.get();
        return current != null ? current.url() : "";
    }
}