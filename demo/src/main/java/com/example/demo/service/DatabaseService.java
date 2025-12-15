package com.example.demo.service;

import com.example.demo.model.MenuProduct;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger; // Import corretto
import java.util.stream.Collectors;

public class DatabaseService {

   private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

   private static final String URL = "jdbc:postgresql://localhost:5432/restaurant_db";
    private static final String USER = "admin";
    private static final String PASS = "password123";

    private DatabaseService() {
        throw new IllegalStateException("Utility class");
    }



    public static List<MenuProduct> getAllProducts() {
        List<MenuProduct> prodotti = new ArrayList<>();
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni FROM menu_items";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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
        logger.info("Tentativo eliminazione prodotto ID: " + id);

        if (id <= 0) {
            logger.log(Level.WARNING, "ID non valido per eliminazione: {0}", id);
            return false;
        }

        String sql = "DELETE FROM menu_items WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("SUCCESSO: Prodotto eliminato.");
                return true;
            } else {
                logger.warning("FALLIMENTO: Nessuna riga trovata con ID: " + id);
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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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
            conn = DriverManager.getConnection(URL, USER, PASS);
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
            for (OrderItem item : items) {
                pstmtItem.setInt(1, orderId);
                pstmtItem.setInt(2, item.getProduct().getId());
                pstmtItem.setInt(3, item.getQuantita());
                pstmtItem.setDouble(4, item.getPrezzoSnapshot());
                pstmtItem.setDouble(5, item.getCostoSnapshot());
                pstmtItem.addBatch();
            }
            pstmtItem.executeBatch();

            conn.commit();
            logger.info("Ordine #" + orderId + " creato con successo.");
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
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY data_ora DESC";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetStatus);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp("data_ora").toLocalDateTime(),
                        rs.getInt("tavolo"),
                        rs.getString("username"),
                        rs.getString("note"),
                        rs.getString("status"),
                        rs.getDouble("totale")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini per stato", e);
        }
        return list;
    }

    public static List<com.example.demo.model.Order> getAllOrdersWithTotal() {
        List<com.example.demo.model.Order> list = new ArrayList<>();
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) as totale_calcolato " +
                "FROM orders o LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora DESC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new com.example.demo.model.Order(
                        rs.getInt("id"),
                        rs.getTimestamp("data_ora").toLocalDateTime(),
                        rs.getInt("tavolo"),
                        rs.getString("username"),
                        rs.getString("note"),
                        rs.getString("status"),
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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "ordered");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp("data_ora").toLocalDateTime(),
                        rs.getInt("tavolo"),
                        rs.getString("username"),
                        rs.getString("note"),
                        rs.getString("status"),
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
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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

    public static List<com.example.demo.model.User> getAllUsers() {
        List<com.example.demo.model.User> list = new ArrayList<>();
        String sql = "SELECT username, role FROM users ORDER BY role, username";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                String u = rs.getString("username");
                String r = rs.getString("role");
                // Qui usiamo la Factory che hai appena corretto!
                com.example.demo.model.User userObj = com.example.demo.app.UsersFactory.createUser(u, r);
                if (userObj != null) list.add(userObj);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero utenti", e);
        }
        return list;
    }

    public static boolean deleteUser(String usernameToDelete) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usernameToDelete);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Utente eliminato: " + usernameToDelete);
                return true;
            } else {
                logger.warning("Utente non trovato per eliminazione: " + usernameToDelete);
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
}