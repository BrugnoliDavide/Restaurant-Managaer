package com.example.rm.service;

import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.preference.KitchenPreferences;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static com.example.rm.service.DBConstants.*;
import java.time.LocalDateTime;

import com.example.rm.preference.PreferencesSerializer;
import com.example.rm.preference.PreferencesConstants;
import java.util.HashMap;
import java.util.Map;


public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    private static String url = null;
    private static String user = null;
    private static String pass = null;

    private static final  String TIPOLOGIASTRING = "tipologia";
    private static final  String TODOSTRING = "to-do";

    private DatabaseService() {
        throw new IllegalStateException("Utility class");
    }

    public static void setConnectionConfig(String ip, String port, String dbName, String username, String password) {
        url = POSTGRES_PREFIX + ip + ":" + port + "/" + dbName;user = username;
        pass = password;
        logger.log(Level.INFO,"Configurazione DB aggiornata: {0}", url);
    }

    public static void setConnectionConfig(
            String ip,
            String port,
            String dbName,
            String username
    ) {
        url = POSTGRES_PREFIX + ip + ":" + port + "/" + dbName;user = username;

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

    public static Map<String, List<MenuProduct>> getMenuByCategories() {
        return getAllProducts().stream()
                .collect(Collectors.groupingBy(MenuProduct::getTipologia));
    }

    public static boolean addProduct(MenuProduct p) {
        String sql = "INSERT INTO menu_items (nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni) " +
                "VALUES (?, ?, ?, ?, ?)";

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
        logger.log(Level.INFO, "Tentativo eliminazione prodotto ID: {0}", id);

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
                logger.log(Level.WARNING, "FALLIMENTO: Nessuna riga trovata con ID: {0}", id);
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE SQL durante eliminazione: {0}, {1}", new Object[]{e.getMessage(), e});
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
                categories.add(rs.getString(TIPOLOGIASTRING));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero categorie", e);
        }
        return categories;
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
    public static boolean createOrder(
            List<OrderItem> items,
            Integer tavolo,
            String note,
            User utente
    ) {

        if (utente == null || items == null || items.isEmpty()) {
            logger.warning("Tentativo di creare ordine non valido");
            return false;
        }

        String usernameUtente = utente.getUsername();

        String sqlOrder =
                "INSERT INTO orders (username, tavolo, note, status) VALUES (?, ?, ?, ?)";

        String sqlItem =
                "INSERT INTO order_items (order_id, menu_item_id, quantita, " +
                        "prezzo_vendita_snapshot, costo_realizzazione_snapshot, nome_prodotto_snapshot) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmtOrder =
                     conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtItem =
                     conn.prepareStatement(sqlItem)) {

            conn.setAutoCommit(false);

            boolean result = executeCreateOrderTransaction(
                    conn, pstmtOrder, pstmtItem,
                    usernameUtente, tavolo, note, items
            );

            conn.commit();
            return result;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore creazione ordine", e);
            return false;
        }
    }




    private static boolean executeCreateOrderTransaction(
            Connection conn,
            PreparedStatement pstmtOrder,
            PreparedStatement pstmtItem,
            String usernameUtente,
            Integer tavolo,
            String note,
            List<OrderItem> items
    ) throws SQLException {

        pstmtOrder.setString(1, usernameUtente);

        if (tavolo != null) {
            pstmtOrder.setInt(2, tavolo);
        } else {
            pstmtOrder.setNull(2, Types.INTEGER);
        }

        pstmtOrder.setString(3, note);
        pstmtOrder.setString(4, TODOSTRING);

        pstmtOrder.executeUpdate();

        int orderId;
        try (ResultSet keys = pstmtOrder.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("ID ordine non generato");
            }
            orderId = keys.getInt(1);
        }

        pstmtItem.setInt(1, orderId);

        for (OrderItem item : items) {
            if (item.getProduct() == null) {
                logger.log(Level.WARNING, "Articolo con prodotto null saltato");
                continue;
            }

            pstmtItem.setInt(2, item.getProduct().getId());
            pstmtItem.setInt(3, item.getQuantita());
            pstmtItem.setDouble(4, item.getPrezzoSnapshot());
            pstmtItem.setDouble(5, item.getCostoSnapshot());
            pstmtItem.setString(6, item.getProduct().getNome());
            pstmtItem.addBatch();
        }

        pstmtItem.executeBatch();


        logger.log(Level.INFO, "Ordine #{0} creato con successo con {1} articoli",
                new Object[]{orderId, items.size()});

        return true;
    }

    private static void handleCreateOrderFailure(Connection conn, SQLException e) {
        try {
            conn.rollback();
        } catch (SQLException rollbackEx) {
            e.addSuppressed(rollbackEx);
        }
        logger.log(Level.SEVERE,
                "Errore durante la creazione dell'ordine, rollback eseguito", e);
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

    public static List<Order> getOrdersByStatus(String statusTarget) {
        List<Order> list = new ArrayList<>();


        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) AS totale_calcolato " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "WHERE o.status = ? " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora DESC";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, statusTarget);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                double totale = rs.getDouble(COL_TOTALE_CALCOLATO);
                Order order = new Order(
                        rs.getInt(COL_ID),
                        rs.getTimestamp(COL_DATA_ORA).toLocalDateTime(),
                        rs.getInt(COL_TAVOLO),
                        rs.getString(COL_USERNAME),
                        rs.getString(COL_NOTE),
                        rs.getString(COL_STATUS),
                        totale
                );
                list.add(order);
            }


        } catch (SQLException e) {
            // Log professionale come discusso precedentemente
            logger.log(Level.SEVERE, "Errore durante il recupero degli ordini con totale: {0}", e.getMessage());
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
                list.add(new Order(
                        rs.getInt(COL_ID),
                        rs.getTimestamp(COL_DATA_ORA).toLocalDateTime(),
                        rs.getInt(COL_TAVOLO),
                        rs.getString(COL_USERNAME),
                        rs.getString(COL_NOTE),
                        rs.getString(COL_STATUS),
                        rs.getDouble(COL_TOTALE_CALCOLATO)
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
            pstmt.setString(1, TODOSTRING);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp(COL_DATA_ORA).toLocalDateTime(),
                        rs.getInt(COL_TAVOLO),
                        rs.getString(COL_USERNAME),
                        rs.getString(COL_NOTE),
                        rs.getString(COL_STATUS),
                        rs.getDouble(COL_TOTALE_CALCOLATO)
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini cucina", e);
        }
        return list;
    }


    /**
     * Recupera gli articoli di un ordine in formato adatto alla visualizzazione.
     * Usa lo snapshot del nome per gestire prodotti eliminati.
     *
     * @param orderId ID dell'ordine
     * @return Lista di stringhe nel formato "Qta x Nome Prodotto"
     */
    public static List<String> getOrderItemsForDisplay(int orderId) {
        List<String> details = new ArrayList<>();

        String sql = "SELECT oi.quantita, " +
                "COALESCE(oi.nome_prodotto_snapshot, mi.nome, 'Prodotto eliminato') AS nome " +
                "FROM order_items oi " +
                "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE oi.order_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int quantita = rs.getInt("quantita");
                String nome = rs.getString("nome");
                details.add(quantita + "x " + nome);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore dettagli ordine {0}, {1}", new Object[]{ orderId, e});
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
        try (Connection conn = getConnection()) {
            logger.info("Connessione al DB riuscita");
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Connessione al DB fallita ", e);
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
        // Recupera gli ordini che sono nello stato 'ready' (pronti ma non pagati)
        return getOrdersByStatus("delivered");
    }

    public static boolean markOrderAsPaid(int orderId) {
        // Cambia lo stato in 'pagato' per farlo sparire dalla cassa
        return setOrderStatus(orderId, "closed");
    }

    public static List<Order> getReadyOrdersForWaiter() {
        return getOrdersByStatus("ready");
    }

    public static boolean markOrderAsDelivered(int orderId) {
        return setOrderStatus(orderId, "delivered");
    }



    public static MenuProduct getProductById(int productId) {
        String sql = "SELECT id, nome, tipologia, prezzovendita, costorealizzazione, allergeni " +
                "FROM menuitems WHERE id = ?";

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
        List<OrderItem> items = new ArrayList<>();

        String sql = "SELECT oi.menu_item_id, " +
                "       oi.quantita, " +
                "       oi.prezzo_vendita_snapshot, " +
                "       oi.costo_realizzazione_snapshot, " +
                "       oi.nome_prodotto_snapshot, " +
                "       mi.id AS product_id, " +
                "       mi.nome AS product_nome, " +
                "       mi.tipologia AS product_tipologia " +
                "FROM order_items oi " +
                "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE oi.order_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int menuItemId = rs.getInt("menu_item_id");
                int quantita = rs.getInt("quantita");
                double prezzoSnap = rs.getDouble("prezzo_vendita_snapshot");
                double costoSnap = rs.getDouble("costo_realizzazione_snapshot");
                String nomeSnap = rs.getString("nome_prodotto_snapshot");

               MenuProduct product;

                Integer productId = rs.getObject("product_id", Integer.class);

                if (productId != null) {
                    product = new MenuProduct();
                    product.setId(productId);
                    product.setNome(rs.getString("product_nome"));
                    product.setTipologia(rs.getString("product_tipologia"));
                } else {
                    product = new MenuProduct();
                    product.setId(menuItemId);
                    product.setNome(nomeSnap != null ? nomeSnap : "Prodotto eliminato");
                    product.setTipologia("Non disponibile");
                }

                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantita(quantita);
                item.setPrezzoSnapshot(prezzoSnap);
                item.setCostoSnapshot(costoSnap);
                item.setNomeSnapshot(nomeSnap);

                items.add(item);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero dettagli articoli ordine {0}, {1}", new Object[]{ orderId, e});
        }

        return items;
    }


    public static long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end) {

        String sql = "SELECT COALESCE(SUM(oi.quantita), 0) " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE oi.menu_item_id = ? " +
                "AND o.data_ora >= ? AND o.data_ora <= ? " +
                "AND o.status != 'canceled'";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            pstmt.setTimestamp(2, Timestamp.valueOf(start));
            pstmt.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore conteggio vendite per data", e);
        }
        return 0;
    }



    /**
     * Carica le preferenze di un utente cucina dalla tabella users.
     * Se non esistono, ritorna preferenze di default.
     * @param username Username della cucina
     * @return KitchenPreferences dell'utente
     */
    public static KitchenPreferences getKitchenPreferences(String username) {
        String sql = "SELECT kitchen_preferences FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

    /**
     * Recupera gli ordini attivi della cucina FILTRATI per categoria.
     * @param username Username dell'utente cucina
     * @return Lista di ordini che rientrano nelle preferenze dell'utente
     */
    public static List<Order> getKitchenActiveOrdersFiltered(String username) {

        KitchenPreferences prefs = getKitchenPreferences(username);

        List<Order> allOrders = getKitchenActiveOrders();

        if (prefs.isIncludeOtherCategories()) {
            return allOrders;
        }

        // Altrimenti, filtra per categorie selezionate
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
     * Scompone un ordine in più ordini per categoria se necessario.
     * Mantiene tutti gli snapshot dei dati originali.
     *
     * @param orderId ID dell'ordine da scomporre
     * @return true se l'operazione è riuscita
     */
    public static boolean decomposeOrderIfNeeded(int orderId) {
        try {
            List<OrderItem> allItems = getOrderItemsDetailed(orderId);

            if (allItems.isEmpty()) {
                return true;
            }

            String sql = "SELECT tavolo, username, note FROM orders WHERE id = ?";

            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, orderId);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) {
                    return false;
                }

                Integer tavolo = rs.getInt("tavolo");
                String username = rs.getString("username");
                String note = rs.getString("note");

                // Raggruppa items per categoria
                Map<String, List<OrderItem>> itemsByCategory = new HashMap<>();
                for (OrderItem item : allItems) {
                    String categoria = item.getProduct().getTipologia();
                    itemsByCategory.computeIfAbsent(categoria, k -> new ArrayList<>()).add(item);
                }

                // Se c'è solo 1 categoria non scomporre
                if (itemsByCategory.size() <= 1) {
                    return true;
                }

                // Crea nuovi ordini per categoria
                try (Connection connNew = DriverManager.getConnection(url, user, pass)) {
                    createCategoryOrders(connNew, itemsByCategory, username, tavolo, note);
                    return true;
                }

            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore scomposizione ordine {0}, errore {1}",
                    new Object[]{orderId, e.getMessage()});
            return false;
        }
    }



    private static void createCategoryOrders(Connection conn,
                                             Map<String, List<OrderItem>> itemsByCategory,
                                             String username,
                                             Integer tavolo,
                                             String note) throws SQLException {

        final String sqlNewOrder =
                "INSERT INTO orders (username, tavolo, note, status) VALUES (?, ?, ?, ?)";

        final String sqlNewItem =
                "INSERT INTO order_items (order_id, menu_item_id, quantita, prezzo_vendita_snapshot, costo_realizzazione_snapshot, nome_prodotto_snapshot) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        boolean previousAutoCommit = conn.getAutoCommit();

        try (PreparedStatement pstmtNewOrder =
                     conn.prepareStatement(sqlNewOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtNewItem =
                     conn.prepareStatement(sqlNewItem)) {

            conn.setAutoCommit(false);

            for (List<OrderItem> categoryItems : itemsByCategory.values()) {

                if (categoryItems == null || categoryItems.isEmpty()) {
                    continue;
                }

                int newOrderId = insertOrderAndGetId(
                        pstmtNewOrder, username, tavolo, note
                );

                pstmtNewItem.setInt(1, newOrderId);

                for (OrderItem item : categoryItems) {
                    pstmtNewItem.setInt(2, item.getProduct().getId());
                    pstmtNewItem.setInt(3, item.getQuantita());
                    pstmtNewItem.setDouble(4, item.getPrezzoSnapshot());
                    pstmtNewItem.setDouble(5, item.getCostoSnapshot());
                    pstmtNewItem.setString(6, item.getNomeSnapshot());
                    pstmtNewItem.addBatch();
                }

                pstmtNewItem.executeBatch();
                pstmtNewItem.clearBatch();
            }

            conn.commit();

        } catch (SQLException e) {
            rollbackQuietly(conn, e);
            throw e;

        } finally {
            restoreAutoCommitQuietly(conn, previousAutoCommit);
        }
    }

    private static int insertOrderAndGetId(PreparedStatement pstmtNewOrder, String username, Integer tavolo, String note) throws SQLException {
        pstmtNewOrder.setString(1, username);
        if (tavolo != null) {
            pstmtNewOrder.setInt(2, tavolo);
        } else {
            pstmtNewOrder.setNull(2, Types.INTEGER);
        }
        pstmtNewOrder.setString(3, note);
        pstmtNewOrder.setString(4, TODOSTRING);

        pstmtNewOrder.executeUpdate();

        try (ResultSet keys = pstmtNewOrder.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("ID non generato per il nuovo ordine");
            }
            return keys.getInt(1);
        }
    }
    private static void rollbackQuietly(Connection conn, SQLException original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackEx) {
            original.addSuppressed(rollbackEx);
        }
    }

    private static void restoreAutoCommitQuietly(Connection conn, boolean previousAutoCommit) {
        try {
            conn.setAutoCommit(previousAutoCommit);
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseService.class.getName())
                    .log(Level.SEVERE, "Impossibile ripristinare autoCommit", ex);
        }
    }






    public static boolean hasPendingOrders(int tavolo) {
        String sql = """
        SELECT EXISTS (
            SELECT 1 FROM orders 
            WHERE tavolo = ? AND status != 'delivered'
        )
        """;

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tavolo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo ordini pendenti tavolo {0}, {1}", new Object[]{tavolo, e});
            return false;
        }
    }

    public static List<Integer> getPendingOrderIds(int tavolo) {
        String sql = """
        SELECT DISTINCT o.id 
        FROM orders o 
        JOIN order_items oi ON o.id = oi.order_id 
        WHERE o.tavolo = ? AND o.status != 'delivered' AND  o.status != 'canceled'
        """;

        List<Integer> pendingIds = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tavolo);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pendingIds.add(rs.getInt(1));
                }
            }
            logger.log(Level.INFO, " Trovati {0} ordini pendenti per tavolo {1}, {2} ", new Object[]{pendingIds.size(), tavolo, pendingIds});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, " ERRORE SQL getPendingOrderIds tavolo {0}: {1} \nQUERY: {2}, {3}", new Object[]{tavolo, e.getMessage(), sql, e});
        }
        return pendingIds;
    }



    public static double getRealizedIncome(
            int productId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        String sql = """
        SELECT COALESCE(
            SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0
        )
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE oi.menu_item_id = ?
          AND o.data_ora BETWEEN ? AND ?
          AND o.status != 'canceled'
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore realized income", e);
        }
        return 0;
    }
}