package com.example.rm.service;

import com.example.rm.dao.DatabaseKitchenPreferencesDAO;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.dao.OrderDAO;
import com.example.rm.dao.impl.OrderDAOPostgres;
import com.example.rm.dao.impl.OrderDAOFile;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.preference.KitchenPreferences;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.rm.service.DBConstants.*;

import java.time.LocalDateTime;
import com.example.rm.preference.PreferencesSerializer;
import com.example.rm.preference.PreferencesConstants;


public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    private static final String TIPOLOGIASTRING = "tipologia";

    /**
     * Record immutabile che aggrega le credenziali di connessione.
     *
     * <p>L'aggregazione in un unico oggetto è la chiave della thread-safety:
     * un singolo campo {@code volatile} garantisce che url, user e pass
     * vengano sempre letti come un'unità coerente. Senza questo accorgimento,
     * tra la scrittura di {@code url} e quella di {@code pass} un thread
     * concorrente potrebbe leggere uno stato parzialmente aggiornato.</p>
     */
    private record ConnectionConfig(String url, String user, String pass) {}

    // volatile garantisce che ogni thread veda sempre l'ultima versione
    // dell'intero oggetto ConnectionConfig, non una versione parzialmente
    // costruita o obsoleta in cache locale.
    private static volatile ConnectionConfig config = null;

    // volatile per la stessa ragione: garantisce visibilità dell'ultima
    // implementazione DAO assegnata (Postgres o FileSystem).
    private static volatile OrderDAO orderDAO = null;


    private DatabaseService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Configura la connessione al database PostgreSQL.
     *
     * <p>Il metodo è {@code synchronized} per evitare che due thread
     * aggiornino contemporaneamente {@code config} e {@code orderDAO},
     * creando uno stato in cui il DAO usa credenziali diverse da quelle
     * appena salvate in {@code config}.</p>
     */
    public static synchronized void setConnectionConfig(String ip, String port, String dbName,
                                                        String username, String password) {
        String url = POSTGRES_PREFIX + ip + ":" + port + "/" + dbName;

        // Scrittura atomica: config e orderDAO vengono aggiornati
        // nell'ordine corretto prima che qualsiasi altro thread possa leggerli.
        config   = new ConnectionConfig(url, username, password);
        orderDAO = new OrderDAOPostgres(url, username, password);

        logger.log(Level.INFO, "Configurazione DB aggiornata: {0}", url);
    }

    /**
     * Configura il sistema per utilizzare il file system per gli ordini.
     *
     * <p>Anch'esso {@code synchronized} per coerenza con {@code setConnectionConfig}:
     * i due metodi non possono essere eseguiti concorrentemente.</p>
     *
     * @param basePath Percorso base per i file degli ordini
     */
    public static synchronized void setFileSystemMode(String basePath) {
        try {
            orderDAO = new OrderDAOFile(basePath);
            logger.log(Level.INFO, "Modalità file system attivata per ordini: {0}", basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile inizializzare file system", e);
        }
    }

    // -------------------------------------------------------------------------
    // Metodi di accesso alla connessione
    // -------------------------------------------------------------------------

    /**
     * Apre e restituisce una nuova connessione JDBC.
     *
     * <p>Legge {@code config} una sola volta in una variabile locale:
     * questo è il pattern corretto con i volatile. Una doppia lettura
     * ({@code config.url()} e poi {@code config.user()}) potrebbe leggere
     * due versioni diverse se un altro thread chiama {@code setConnectionConfig}
     * nel mezzo. La lettura singola in {@code current} evita questa race.</p>
     */
    public static Connection getConnection() throws SQLException {
        ConnectionConfig current = config; // lettura volatile una sola volta
        if (current == null) {
            throw new IllegalStateException("Database non configurato correttamente");
        }
        return DriverManager.getConnection(current.url(), current.user(), current.pass());
    }

    public static boolean isConfigured() {
        return config != null;
    }

    public static boolean testConnection() {
        if (config == null) {
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

    public static void loadFromPreferences() {
        String host = DBConfigStore.getHost();
        String port = DBConfigStore.getPort();
        String db   = DBConfigStore.getDbName();
        String user = DBConfigStore.getUser();
        String pass = DBConfigStore.getPassword();

        if (!host.isBlank() && !port.isBlank() && !db.isBlank() && !user.isBlank()) {
            setConnectionConfig(host, port, db, user, pass);
        }
    }

    // -------------------------------------------------------------------------
    // Metodi di accesso ai parametri di connessione (per la UI di configurazione)
    // Leggono config una sola volta per coerenza.
    // -------------------------------------------------------------------------

    public static String getDBHost() {
        ConnectionConfig current = config;
        if (current == null) return "";
        String noPrefix = current.url().replace(POSTGRES_PREFIX, "");
        return noPrefix.substring(0, noPrefix.indexOf(":"));
    }

    public static String getDBPort() {
        ConnectionConfig current = config;
        if (current == null) return "";
        String noPrefix = current.url().replace(POSTGRES_PREFIX + getDBHost(), "");
        int start = noPrefix.indexOf(":") + 1;
        int end   = noPrefix.indexOf("/");
        return noPrefix.substring(start, end);
    }

    public static String getDBName() {
        ConnectionConfig current = config;
        if (current == null) return "";
        return current.url().substring(current.url().lastIndexOf("/") + 1);
    }

    public static String getDBUser() {
        ConnectionConfig current = config;
        return current != null ? current.user() : "";
    }

    public static boolean hasPassword() {
        ConnectionConfig current = config;
        return current != null && current.pass() != null && !current.pass().isBlank();
    }

    // -------------------------------------------------------------------------
    // Helper per verificare che il DAO sia inizializzato prima dell'uso
    // -------------------------------------------------------------------------

    private static OrderDAO requireOrderDAO() {
        OrderDAO dao = orderDAO; // lettura volatile una sola volta
        if (dao == null) {
            throw new IllegalStateException(
                    "DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode."
            );
        }
        return dao;
    }

    // -------------------------------------------------------------------------
    // Operazioni sui prodotti del menu
    // -------------------------------------------------------------------------

    public static List<MenuProduct> getAllProducts() {
        List<MenuProduct> prodotti = new ArrayList<>();
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni FROM menu_items";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                prodotti.add(new MenuProduct(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString(TIPOLOGIASTRING),
                        rs.getDouble("prezzo_vendita"),
                        rs.getDouble("costo_realizzazione"),
                        rs.getString("allergeni")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti", e);
        }
        return prodotti;
    }

    public static boolean deleteProduct(int id) {
        return DatabaseProductDAO.deleteProduct(id);
    }

    public static MenuProduct getProductById(int productId) {
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni " +
                "FROM menu_items WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MenuProduct(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString(TIPOLOGIASTRING),
                            rs.getDouble("prezzo_vendita"),
                            rs.getDouble("costo_realizzazione"),
                            rs.getString("allergeni")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il recupero del prodotto con ID {0}", productId);
        }
        return null;
    }

    public static long getQuantitySold(String nomeProdotto) {
        String sql = "SELECT SUM(oi.quantita) FROM order_items oi " +
                "JOIN menu_items mi ON oi.menu_item_id = mi.id WHERE mi.nome = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomeProdotto);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore conteggio vendite", e);
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Operazioni sugli utenti
    // -------------------------------------------------------------------------

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT username, role FROM users ORDER BY role, username";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String u = rs.getString(COL_USERNAME);
                String r = rs.getString(COL_ROLE);
                User userObj = com.example.rm.app.UsersFactory.createUser(u, r);
                if (userObj != null) list.add(userObj);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero utenti", e);
        }
        return list;
    }

    public static boolean deleteUser(String usernameToDelete) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usernameToDelete);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Utente: {0} eliminato.", usernameToDelete);
                return true;
            } else {
                logger.log(Level.WARNING, "Utente non trovato per eliminazione: {0}", usernameToDelete);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore eliminazione utente", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Operazioni sugli ordini — delegate al DAO
    // -------------------------------------------------------------------------

    public static boolean createOrder(List<OrderItem> items, Integer tavolo, String note, User utente) {
        return requireOrderDAO().createOrder(items, tavolo, note, utente);
    }

    public static boolean setOrderStatus(int orderId, String newStatus) {
        return requireOrderDAO().setOrderStatus(orderId, newStatus);
    }

    public static List<Order> getAllOrdersWithTotal() {
        return requireOrderDAO().getAllOrdersWithTotal();
    }

    public static List<Order> getKitchenActiveOrders() {
        return requireOrderDAO().getKitchenActiveOrders();
    }

    public static List<String> getOrderItemsForDisplay(int orderId) {
        return requireOrderDAO().getOrderItemsForDisplay(orderId);
    }

    public static List<OrderItem> getOrderItemsDetailed(int orderId) {
        return requireOrderDAO().getOrderItemsDetailed(orderId);
    }

    public static List<Order> getOrdersToPay() {
        return requireOrderDAO().getOrdersToPay();
    }

    public static boolean markOrderAsPaid(int orderId) {
        return requireOrderDAO().markOrderAsPaid(orderId);
    }

    public static List<Order> getReadyOrdersForWaiter() {
        return requireOrderDAO().getReadyOrdersForWaiter();
    }

    public static boolean markOrderAsDelivered(int orderId) {
        return requireOrderDAO().markOrderAsDelivered(orderId);
    }

    public static long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end) {
        return requireOrderDAO().getQuantitySoldInDateRange(productId, start, end);
    }

    public static boolean decomposeOrderIfNeeded(int orderId) {
        return requireOrderDAO().decomposeOrderIfNeeded(orderId);
    }

    public static boolean hasPendingOrders(int tavolo) {
        return requireOrderDAO().hasPendingOrders(tavolo);
    }

    public static List<Integer> getPendingOrderIds(int tavolo) {
        return requireOrderDAO().getPendingOrderIds(tavolo);
    }

    // -------------------------------------------------------------------------
    // Preferenze cucina
    // -------------------------------------------------------------------------

    public static KitchenPreferences getKitchenPreferences(String username) {
        return DatabaseKitchenPreferencesDAO.getKitchenPreferences(username);
    }

    public static List<Order> getKitchenActiveOrdersFiltered(String username) {
        KitchenPreferences prefs = getKitchenPreferences(username);
        List<Order> allOrders = getKitchenActiveOrders();

        if (prefs.isIncludeOtherCategories()) {
            return allOrders;
        }

        List<Order> filteredOrders = new ArrayList<>();
        Set<String> selectedCategories = prefs.getSelectedCategories();

        for (Order order : allOrders) {
            List<OrderItem> items = getOrderItemsDetailed(order.getId());
            Set<String> orderCategories = extractCategoriesFromItems(items);
            if (orderCategories.stream().allMatch(selectedCategories::contains)) {
                filteredOrders.add(order);
            }
        }
        return filteredOrders;
    }

    private static Set<String> extractCategoriesFromItems(List<OrderItem> items) {
        Set<String> categories = new HashSet<>();
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getProduct() != null && item.getProduct().getTipologia() != null) {
                    categories.add(item.getProduct().getTipologia());
                }
            }
        }
        return categories;
    }

    // -------------------------------------------------------------------------
    // Costanti esposte (usate da altri layer — da migrare in DBConstants)
    // -------------------------------------------------------------------------

    /** @deprecated Usare DBConstants.TIPOLOGIA_COLUMN direttamente */
    @Deprecated(since = "refactoring-2026", forRemoval = true)
    public static String returnTIPOLOGIASTRING() {
        return TIPOLOGIASTRING;
    }
}