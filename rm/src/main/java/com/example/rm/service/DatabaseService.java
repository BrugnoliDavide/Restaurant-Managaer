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



public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    private static String url = null;
    private static String user = null;
    private static String pass = null;

    private static final String TIPOLOGIASTRING = "tipologia";

    private static OrderDAO orderDAO = null;


    private DatabaseService() {
        throw new IllegalStateException("Utility class");
    }

    public static void setConnectionConfig(String ip, String port, String dbName, String username, String password) {
        url = POSTGRES_PREFIX + ip + ":" + port + "/" + dbName;user = username;
        pass = password;

        orderDAO = new OrderDAOPostgres(url, user, password);

        logger.log(Level.INFO,"Configurazione DB aggiornata: {0}", url);
    }

    public static List<MenuProduct> getAllProducts() {
        List<MenuProduct> prodotti = new ArrayList<>();
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni FROM menu_items";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
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

    /**
     * Crea un nuovo ordine con i suoi articoli.
     * Salva uno snapshot completo dei dati del prodotto al momento dell'ordine.
     *
     * @param items Lista di articoli da ordinare
     * @param tavolo Numero tavolo (può essere null)
     * @param note Note aggiuntive (può essere null)
     * @param utente Utente che crea l'ordine
     * @return true se l'ordine è stato creato con successo
     */
    public static boolean createOrder(List<OrderItem> items, Integer tavolo, String note, User utente) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.createOrder(items, tavolo, note, utente);
    }


    public static long getQuantitySold(String nomeProdotto) {
        String sql = "SELECT SUM(oi.quantita) FROM order_items oi JOIN menu_items mi ON oi.menu_item_id = mi.id WHERE mi.nome = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomeProdotto);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore conteggio vendite", e);
        }
        return 0;
    }

    public static boolean setOrderStatus(int orderId, String newStatus) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.setOrderStatus(orderId, newStatus);
    }


    public static List<Order> getAllOrdersWithTotal() {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getAllOrdersWithTotal();
    }

    public static List<Order> getKitchenActiveOrders() {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getKitchenActiveOrders();
    }

    /**
     * Recupera gli articoli di un ordine in formato adatto alla visualizzazione.
     * Usa lo snapshot del nome per gestire prodotti eliminati.
     *
     * @param orderId ID dell'ordine
     * @return Lista di stringhe nel formato "Qta x Nome Prodotto"
     */
    public static List<String> getOrderItemsForDisplay(int orderId) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getOrderItemsForDisplay(orderId);
    }

    public static List<com.example.rm.model.User> getAllUsers() {
        List<com.example.rm.model.User> list = new ArrayList<>();
        String sql = "SELECT username, role FROM users ORDER BY role, username";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String u = rs.getString(COL_USERNAME);
                String r = rs.getString(COL_ROLE);
                com.example.rm.model.User userObj = com.example.rm.app.UsersFactory.createUser(u, r);
                if (userObj != null) list.add(userObj);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero utenti", e);
        }
        return list;
    }

    public static boolean deleteUser(String usernameToDelete) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usernameToDelete);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO,"Utente: {0} eliminato.", usernameToDelete);
                return true;
            } else {
                logger.log(Level.WARNING,"Utente non trovato per eliminazione: {0}",usernameToDelete);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore eliminazione utente ", e);
            return false;
        }
    }

    //metodi necessari per mostrare all'utente i dati di connessione usati l'ultima volta e permettervi la modifica
    public static String getDBHost() {
        if (url == null) return "";
        String noPrefix = url.replace(POSTGRES_PREFIX, "");
        return noPrefix.substring(0, noPrefix.indexOf(":"));
    }

    public static String getDBPort() {
        if (url == null) return "";
        String noPrefix = url.replace(POSTGRES_PREFIX + getDBHost(), "");
        int start = noPrefix.indexOf(":") + 1;
        int end = noPrefix.indexOf("/");
        return noPrefix.substring(start, end);
    }

    public static String getDBName(){
        if (url == null) return "";
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public static String getDBUser(){
        if (url == null) return "";
        return user;
    }

    public static boolean hasPassword() {
        return pass != null && !pass.isBlank();
    }

    public static boolean isConfigured() {
        return url != null && user != null && pass != null;
    }

    public static boolean testConnection() {
        if (url == null || user == null || pass == null) {
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

    public static Connection getConnection() throws SQLException {

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException(
                    "Database non configurato correttamente"
            );
        }

        return DriverManager.getConnection(url, user, pass);
    }

    public static List<Order> getOrdersToPay() {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getOrdersToPay();
    }

    public static boolean markOrderAsPaid(int orderId) {
        // Cambia lo stato in 'pagato' per farlo sparire dalla cassa
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.markOrderAsPaid(orderId);
    }

    public static List<Order> getReadyOrdersForWaiter() {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getReadyOrdersForWaiter();
    }

    public static boolean markOrderAsDelivered(int orderId) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.markOrderAsDelivered(orderId);
    }

    public static MenuProduct getProductById(int productId) {
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni " +
                "FROM menu_items WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MenuProduct(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString(TIPOLOGIASTRING),
                            rs.getDouble("prezzovendita"),
                            rs.getDouble("costorealizzazione"),
                            rs.getString("allergeni")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il recupero del prodotto con ID {0}", productId);
        }

        return null;
    }


    /**
     * Recupera gli articoli dettagliati di un ordine.
     * Gestisce anche i prodotti eliminati usando gli snapshot.
     *
     * @param orderId ID dell'ordine
     * @return Lista di OrderItem con dati completi
     */
    public static List<OrderItem> getOrderItemsDetailed(int orderId) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getOrderItemsDetailed(orderId);
    }

    public static long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.getQuantitySoldInDateRange(productId, start, end);
    }

    // !! spostato nel DAO !! da deprecare (o trasformare in un richiamo al DAO)
    /**
     * Carica le preferenze di un utente cucina dalla tabella users.
     * Se non esistono, ritorna preferenze di default.
     * @param username Username della cucina
     * @return KitchenPreferences dell'utente
     */
    public static KitchenPreferences getKitchenPreferences(String username) {
        return DatabaseKitchenPreferencesDAO.getKitchenPreferences(username);
    }


    /**
     * recupera gli ordini attivi della cucina FILTRATI per categoria.
     * @param username Username dell'utente cucina
     * @return Lista di ordini che rientrano nelle preferenze dell'utente
     */
    public static List<Order> getKitchenActiveOrdersFiltered(String username) {

        KitchenPreferences prefs = getKitchenPreferences(username);

        List<Order> allOrders = getKitchenActiveOrders();

        if (prefs.isIncludeOtherCategories()) {
            return allOrders;
        }

        List<Order> filteredOrders = new ArrayList<>();
        Set<String> selectedCategories = prefs.getSelectedCategories();

        for (Order order : allOrders) {
            // Estrai le categorie di questo ordine
            List<OrderItem> items = getOrderItemsDetailed(order.getId());
            Set<String> orderCategories = extractCategoriesFromItems(items);

            // Se tutte le categorie dell'ordine sono selezionate, includilo
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

    /**
     * scompone un ordine in più ordini per categoria
     * mantiene tutti gli snapshot dei dati originali
     *
     * @param orderId ID dell'ordine da scomporre
     * @return true se l'operazione è riuscita
     */
    public static boolean decomposeOrderIfNeeded(int orderId) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.decomposeOrderIfNeeded(orderId);
    }

    public static boolean hasPendingOrders(int tavolo) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato. Chiamare setConnectionConfig o setFileSystemMode.");
        }
        return orderDAO.hasPendingOrders(tavolo);
    }

    public static List<Integer> getPendingOrderIds(int tavolo) {
        if (orderDAO == null) {
            throw new IllegalStateException("DAO ordini non inizializzato");
        }
        return orderDAO.getPendingOrderIds(tavolo);
    }

    /**
     * Configura il sistema per utilizzare il file system invece del database per gli ordini.
     * @param basePath Percorso base per i file degli ordini
     */
    public static void setFileSystemMode(String basePath) {
        try {
            orderDAO = new OrderDAOFile(basePath);
            logger.log(Level.INFO, "Modalità file system attivata per ordini: {0}", basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile inizializzare file system", e);
        }
    }

    public static String returnTIPOLOGIASTRING(){
        return TIPOLOGIASTRING;
    }
}