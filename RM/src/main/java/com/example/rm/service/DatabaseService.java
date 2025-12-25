package com.example.rm.service;

import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger; // Import corretto
import java.util.stream.Collectors;

public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    private static String url = null;
    private static String user = null;
    private static String pass = null;

    private static String username = "username";
    private static String status = "status";
    private static String dataOra = "data_ora";

    //!! riabilitare quando e se necessario
    //private static String OrderId = "orderid";
    private static String table = "tavolo";

    private static String postgressConnectionPrefix = "jdbc:postgresql://";


    private DatabaseService() {
        throw new IllegalStateException("Utility class");
    }

    public static void setConnectionConfig(String ip, String port, String dbName, String username, String password) {
        url = postgressConnectionPrefix + ip + ":" + port + "/" + dbName;
        user = username;
        pass = password;
        logger.log(Level.INFO,"Configurazione DB aggiornata: {0}", url);
    }

    public static void setConnectionConfig(
            String ip,
            String port,
            String dbName,
            String username
    ) {
        url  = postgressConnectionPrefix + ip + ":" + port + "/" + dbName;
        user = username;
        // la PASSWORD resta invariata
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
                        rs.getString("tipologia"),
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

    public static Map<String, List<MenuProduct>> getMenuByCategories() {
        return getAllProducts().stream()
                .collect(Collectors.groupingBy(MenuProduct::getTipologia));
    }

    public static boolean addProduct(MenuProduct p) {
        String sql = "INSERT INTO menu_items (nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getTipologia());
            pstmt.setDouble(3, p.getPrezzoVendita());
            pstmt.setDouble(4, p.getCostoRealizzazione());
            pstmt.setString(5, p.getAllergeni());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE INSERIMENTO PRODOTTO", e);
            return false;
        }
    }

    public static boolean updateProduct(MenuProduct p) {
        String sql = "UPDATE menu_items SET nome = ?, tipologia = ?, prezzo_vendita = ?, costo_realizzazione = ?, allergeni = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getTipologia());
            pstmt.setDouble(3, p.getPrezzoVendita());
            pstmt.setDouble(4, p.getCostoRealizzazione());
            pstmt.setString(5, p.getAllergeni());
            pstmt.setInt(6, p.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE UPDATE PRODOTTO", e);
            return false;
        }
    }

    public static boolean deleteProduct(int id) {
        logger.log(Level.INFO,"Tentativo eliminazione prodotto ID: {0}", id);

        if (id <= 0) {
            logger.log(Level.WARNING, "ID non valido per eliminazione: {0}", id);
            return false;
        }

        String sql = "DELETE FROM menu_items WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("SUCCESSO: Prodotto eliminato.");
                return true;
            } else {

                logger.log(Level.WARNING,"FALLIMENTO: Nessuna riga trovata con ID: {0}", id);

                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE SQL GRAVE durante eliminazione", e);
            return false;
        }
    }

    public static List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT tipologia FROM menu_items ORDER BY tipologia";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(rs.getString("tipologia"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero categorie", e);
        }

        if (categories.isEmpty()) {
            categories.add("Primi");
            categories.add("Secondi");
            categories.add("Bibite");
        }
        return categories;
    }

    public static boolean createOrder(List<OrderItem> items, Integer tavolo, String note) {
        if (items.isEmpty()) return false;

        String sqlOrder = "INSERT INTO orders (username, tavolo, note, status) VALUES (?,?, ?, ?)";
        String sqlItem = "INSERT INTO order_items (order_id, menu_item_id, quantita, prezzo_vendita_snapshot, costo_realizzazione_snapshot) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmtOrder = null;
        PreparedStatement pstmtItem = null;

        try {
            conn = DriverManager.getConnection(url, user, pass);
            conn.setAutoCommit(false); // Transazione

            // A. Testata Ordine
            pstmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
            pstmtOrder.setString(1, "Manager");
            if (tavolo != null) pstmtOrder.setInt(2, tavolo);
            else pstmtOrder.setNull(2, Types.INTEGER);
            pstmtOrder.setString(3, note);
            pstmtOrder.setString(4, "ordered");

            if (pstmtOrder.executeUpdate() == 0) throw new SQLException("Creazione ordine fallita.");

            int orderId = 0;
            try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Fallito recupero ID ordine.");
                }
            }

            // B. Righe Ordine
            pstmtItem = conn.prepareStatement(sqlItem);

            pstmtItem.setInt(1, orderId);

            for (OrderItem item : items) {
                pstmtItem.setInt(2, item.getProduct().getId());
                pstmtItem.setInt(3, item.getQuantita());
                pstmtItem.setDouble(4, item.getPrezzoSnapshot());
                pstmtItem.setDouble(5, item.getCostoSnapshot());
                pstmtItem.addBatch();
            }
            pstmtItem.executeBatch();

            conn.commit();
            logger.log(Level.INFO,"Ordine # {0} creato con successo.",orderId);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    logger.warning("Rollback in corso...");
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Errore durante il Rollback", ex);
                }
            }
            logger.log(Level.SEVERE, "Errore creazione ordine", e);
            return false;
        } finally {
            closeQuietly(pstmtItem);
            closeQuietly(pstmtOrder);
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Errore chiusura connessione", e);
            }
        }
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
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore cambio stato ordine", e);
            return false;
        }
    }

    public static List<Order> getOrdersByStatus(String targetStatus) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT id, data_ora, tavolo, username, note, status, totale FROM orders WHERE status = ? ORDER BY data_ora DESC";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetStatus);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp(dataOra).toLocalDateTime(),
                        rs.getInt(table),
                        rs.getString(username),
                        rs.getString("note"),
                        rs.getString(status),
                        rs.getDouble("totale")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini per stato", e);
        }
        return list;
    }

    public static List<com.example.rm.model.Order> getAllOrdersWithTotal() {
        List<com.example.rm.model.Order> list = new ArrayList<>();
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) as totale_calcolato " +
                "FROM orders o LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora DESC";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new com.example.rm.model.Order(
                        rs.getInt("id"),
                        rs.getTimestamp(dataOra).toLocalDateTime(),
                        rs.getInt(table),
                        rs.getString(username),
                        rs.getString("note"),
                        rs.getString(status),
                        rs.getDouble("totale_calcolato")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini con totale", e);
        }
        return list;
    }

    public static List<Order> getKitchenActiveOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) as totale_calcolato " +
                "FROM orders o LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "WHERE o.status = ? AND o.data_ora >= NOW() - INTERVAL '24 HOURS' " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora ASC";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "ordered");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp(dataOra).toLocalDateTime(),
                        rs.getInt(table),
                        rs.getString(username),
                        rs.getString("note"),
                        rs.getString(status),
                        rs.getDouble("totale_calcolato")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini cucina", e);
        }
        return list;
    }

    public static List<String> getOrderItemsForDisplay(int orderId) {
        List<String> details = new ArrayList<>();
        String sql = "SELECT mi.nome, oi.quantita FROM order_items oi JOIN menu_items mi ON oi.menu_item_id = mi.id WHERE oi.order_id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                details.add(rs.getInt("quantita") + "x " + rs.getString("nome"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore dettagli ordine", e);
        }
        return details;
    }

    public static List<com.example.rm.model.User> getAllUsers() {
        List<com.example.rm.model.User> list = new ArrayList<>();
        String sql = "SELECT username, role FROM users ORDER BY role, username";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String u = rs.getString("username");
                String r = rs.getString("role");
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
            logger.log(Level.SEVERE, "Errore eliminazione utente", e);
            return false;
        }
    }

    // Metodo helper per chiudere le risorse senza sporcare il codice con try-catch
    private static void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) resource.close();
        } catch (Exception e) {
            logger.warning("Chiusura risorsa ignorata: " + e.getMessage());
        }
    }


    //metodi necessari per mostrare all'utente i dati di connessione usati l'ultima volta e permettervi la modifica
    public static String getDBHost() {
        if (url == null) return "";
        String noPrefix = url.replace(postgressConnectionPrefix, "");
        return noPrefix.substring(0, noPrefix.indexOf(":"));
    }

    public static String getDBPort() {
        if (url == null) return "";
        String noPrefix = url.replace(postgressConnectionPrefix+getDBHost(), "");
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

    public static String getDBPassword() {
        return pass;
    }



    public static boolean isConfigured() {
        return url != null && user != null && pass != null;
    }

    public static boolean testConnection() {
        if (url == null || user == null || pass == null) {
            logger.warning("Tentativo test DB senza configurazione completa");
            return false;
        }

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
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





}