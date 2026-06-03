package rm.dao.impl;

import rm.bean.OrderBean;
import rm.bean.OrderItemBean;
import rm.dao.OrderDAO;
import rm.model.MenuProduct;
import rm.model.OrderItem;
import rm.model.User;
import rm.service.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rm.service.DBConstants.*;

/**
 * Implementazione PostgreSQL di {@link OrderDAO}.
 *
 * <p>Tutti i metodi di lettura restituiscono Bean; la conversione a Model
 * avviene nel layer Service tramite {@code BeanMapper}.</p>
 *
 * <p>L'eccezione è {@link #getItemsWithTipologiaForDecomposition(int)},
 * metodo privato usato internamente dalla scomposizione ordini: richiede
 * la {@code tipologia} del prodotto (non presente nello snapshot) e perciò
 * restituisce {@code List<OrderItem>} con il campo product popolato.</p>
 */
public class OrderDAOPostgres implements OrderDAO {

    private static final Logger logger = Logger.getLogger(OrderDAOPostgres.class.getName());
    private static final String TODOSTRING =                "to-do";
    private static final String QTASTR =                    "quantita";
    private static final String NOMEPRODOTTOSNAPSHOTSTR =   "nome_prodotto_snapshot";

    @FunctionalInterface
    private interface TransactionalOperation {
        boolean execute() throws SQLException;
    }

    private Connection getConnection() throws SQLException {
        return ConnectionManager.getConnection();
    }

    private boolean commitOrRollback(Connection conn,
                                     TransactionalOperation operation) throws SQLException {
        try {
            boolean result = operation.execute();
            conn.commit();
            return result;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Creazione ordine
    // -------------------------------------------------------------------------

    @Override
    public boolean createOrder(List<OrderItem> items, Integer tavolo, String note, User utente) {
        if (utente == null || items == null || items.isEmpty()) {
            logger.warning("Tentativo di creare ordine non valido");
            return false;
        }

        String sqlOrder = "INSERT INTO orders (username, tavolo, note, status) VALUES (?, ?, ?, ?)";
        String sqlItem  = "INSERT INTO order_items (order_id, menu_item_id, quantita, " +
                "prezzo_vendita_snapshot, costo_realizzazione_snapshot, nome_prodotto_snapshot) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtItem  = conn.prepareStatement(sqlItem)) {

            conn.setAutoCommit(false);
            return commitOrRollback(conn,
                    () -> executeCreateOrderTransaction(
                            pstmtOrder, pstmtItem,
                            utente.getUsername(), tavolo, note, items));

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore creazione ordine", e);
            return false;
        }
    }

    private boolean executeCreateOrderTransaction(PreparedStatement pstmtOrder,
                                                  PreparedStatement pstmtItem,
                                                  String username,
                                                  Integer tavolo,
                                                  String note,
                                                  List<OrderItem> items) throws SQLException {
        pstmtOrder.setString(1, username);
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
            if (!keys.next()) throw new SQLException("ID ordine non generato");
            orderId = keys.getInt(1);
        }

        pstmtItem.setInt(1, orderId);
        for (OrderItem item : items) {
            if (item.getProduct() == null) {
                logger.warning("Articolo con prodotto null saltato");
                continue;
            }
            pstmtItem.setInt(2, item.getProduct().getId());
            pstmtItem.setInt(3, item.getQuantita());
            pstmtItem.setBigDecimal(4, item.getPrezzoSnapshot());
            pstmtItem.setBigDecimal(5, item.getCostoSnapshot());
            pstmtItem.setString(6, item.getProduct().getNome());
            pstmtItem.addBatch();
        }
        pstmtItem.executeBatch();

        logger.log(Level.INFO, "Ordine #{0} creato con {1} articoli",
                new Object[]{orderId, items.size()});
        return true;
    }

    // -------------------------------------------------------------------------
    // Query ordini — restituiscono List<OrderBean>
    // -------------------------------------------------------------------------

    @Override
    public List<OrderBean> getAllOrdersWithTotal() {
        List<OrderBean> list = new ArrayList<>();
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) AS totale_calcolato " +
                "FROM orders o LEFT JOIN order_items oi " +
                "    ON o.id = oi.order_id AND oi.status <> 'canceled' " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini con totale", e);
        }
        return list;
    }

    @Override
    public List<OrderBean> getOrdersByStatus(String statusTarget) {
        List<OrderBean> list = new ArrayList<>();
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) AS totale_calcolato " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id AND oi.status <> 'canceled' " +
                "WHERE o.status = ? " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, statusTarget);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini per stato", e);
        }
        return list;
    }

    @Override
    public List<OrderBean> getKitchenActiveOrders() {
        List<OrderBean> list = new ArrayList<>();
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) AS totale_calcolato " +
                "FROM orders o LEFT JOIN order_items oi " +
                "    ON o.id = oi.order_id AND oi.status <> 'canceled' " +
                "WHERE o.status = ? AND o.data_ora >= NOW() - INTERVAL '24 HOURS' " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, TODOSTRING);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini cucina", e);
        }
        return list;
    }

    @Override
    public List<OrderBean> getReadyOrdersForWaiter() {
        return getOrdersByStatus("ready");
    }

    @Override
    public List<OrderBean> getOrdersToPay() {
        return getOrdersByStatus("delivered");
    }

    @Override
    public OrderBean findById(int orderId) {
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) AS totale_calcolato " +
                "FROM orders o LEFT JOIN order_items oi " +
                "    ON o.id = oi.order_id AND oi.status <> 'canceled' " +
                "WHERE o.id = ? " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapOrder(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ordine per ID: {0}", orderId);
        }
        return null;
    }

    /** Mappa la riga corrente del ResultSet in un OrderBean. */
    private OrderBean mapOrder(ResultSet rs) throws SQLException {
        return new OrderBean(
                rs.getInt(COL_ID),
                rs.getTimestamp(COL_DATA_ORA).toLocalDateTime(),
                rs.getInt(COL_TAVOLO),
                rs.getString(COL_USERNAME),
                rs.getString(COL_NOTE),
                rs.getString(COL_STATUS),
                rs.getBigDecimal(COL_TOTALE_CALCOLATO)
        );
    }

    // -------------------------------------------------------------------------
    // Stato ordine
    // -------------------------------------------------------------------------

    @Override
    public boolean setOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore cambio stato ordine", e);
            return false;
        }
    }

    @Override
    public boolean markOrderAsDelivered(int orderId) {
        return setOrderStatus(orderId, "delivered");
    }

    @Override
    public boolean markOrderAsPaid(int orderId) {
        return setOrderStatus(orderId, "closed");
    }

    // -------------------------------------------------------------------------
    // Dettagli articoli — restituisce List<OrderItemBean>
    // -------------------------------------------------------------------------

    @Override
    public List<OrderItemBean> getOrderItemsDetailed(int orderId) {
        List<OrderItemBean> items = new ArrayList<>();
        String sql = "SELECT oi.id AS oi_id, oi.order_id, oi.status AS oi_status, " +
                "oi.menu_item_id, oi.quantita, oi.prezzo_vendita_snapshot, " +
                "oi.costo_realizzazione_snapshot, oi.nome_prodotto_snapshot " +
                "FROM order_items oi " +
                "WHERE oi.order_id = ? AND oi.status <> 'canceled'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapOrderItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero dettagli articoli ordine", e);
        }
        return items;
    }

    /** Mappa la riga corrente del ResultSet in un OrderItemBean. */
    private OrderItemBean mapOrderItem(ResultSet rs) throws SQLException {


        return  new OrderItemBean(
                rs.getInt("order_id"),
                rs.getInt("menu_item_id"),
                rs.getInt(QTASTR),
                rs.getBigDecimal("prezzo_vendita_snapshot"),
                rs.getBigDecimal("costo_realizzazione_snapshot"),
                rs.getString(NOMEPRODOTTOSNAPSHOTSTR)
        );
    }

    /**
     * Metodo privato esclusivo per la decomposizione ordini.
     * Recupera gli articoli con la {@code tipologia} del prodotto (non presente
     * nello snapshot) facendo JOIN su {@code menu_items}.
     * Restituisce {@code List<OrderItem>} con il campo {@code product} popolato.
     */
    private List<OrderItem> getItemsWithTipologiaForDecomposition(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.id AS oi_id, oi.menu_item_id, oi.quantita, " +
                "oi.prezzo_vendita_snapshot, oi.costo_realizzazione_snapshot, " +
                "oi.nome_prodotto_snapshot, " +
                "mi.id AS product_id, mi.nome AS product_nome, mi.tipologia AS product_tipologia " +
                "FROM order_items oi " +
                "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE oi.order_id = ? AND oi.status <> 'canceled'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Integer productId = rs.getObject("product_id", Integer.class);
                    MenuProduct product = new MenuProduct();
                    if (productId != null) {
                        product.setId(productId);
                        product.setNome(rs.getString("product_nome"));
                        product.setTipologia(rs.getString("product_tipologia"));
                    } else {
                        product.setId(rs.getInt("menu_item_id"));
                        String nomeSnap = rs.getString(NOMEPRODOTTOSNAPSHOTSTR);
                        product.setNome(nomeSnap != null ? nomeSnap : "Prodotto eliminato");
                        product.setTipologia("Altro");
                    }
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("oi_id"));
                    item.setProduct(product);
                    item.setQuantita(rs.getInt(QTASTR));
                    item.setPrezzoSnapshot(rs.getBigDecimal("prezzo_vendita_snapshot"));
                    item.setCostoSnapshot(rs.getBigDecimal("costo_realizzazione_snapshot"));
                    item.setNomeSnapshot(rs.getString(NOMEPRODOTTOSNAPSHOTSTR));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero articoli per decomposizione ordine {0}", orderId);
        }
        return items;
    }

    @Override
    public List<String> getOrderItemsForDisplay(int orderId) {
        List<String> details = new ArrayList<>();
        String sql = "SELECT oi.quantita, " +
                "COALESCE(oi.nome_prodotto_snapshot, mi.nome, 'Prodotto eliminato') AS nome " +
                "FROM order_items oi " +
                "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE oi.order_id = ? AND oi.status <> 'canceled'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(rs.getInt(QTASTR) + "x " + rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore dettagli ordine per display", e);
        }
        return details;
    }

    @Override
    public Map<Integer, List<String>> getAllOrderItemsForDisplay() {
        Map<Integer, List<String>> result = new HashMap<>();
        String sql = "SELECT oi.order_id, oi.quantita, " +
                "COALESCE(oi.nome_prodotto_snapshot, mi.nome, 'Prodotto eliminato') AS nome " +
                "FROM order_items oi " +
                "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE oi.status <> 'canceled' " +
                "ORDER BY oi.order_id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                String display = rs.getInt(QTASTR) + "x " + rs.getString("nome");
                result.computeIfAbsent(orderId, k -> new ArrayList<>()).add(display);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento items per display", e);
        }
        return result;
    }

    @Override
    public boolean removeOrderItem(int orderId, int itemId) {
        String sql = "DELETE FROM order_items WHERE id = ? AND order_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ps.setInt(2, orderId);
            int affected = ps.executeUpdate();
            logger.log(Level.INFO, "Rimosso item {0} da ordine {1}: {2} righe",
                    new Object[]{itemId, orderId, affected});
            return affected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore rimozione item", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Scomposizione ordine
    // -------------------------------------------------------------------------

    @Override
    public boolean decomposeOrderIfNeeded(int orderId) {
        // Usa il metodo interno con tipologia — necessario per raggruppare per categoria
        List<OrderItem> allItems = getItemsWithTipologiaForDecomposition(orderId);
        if (allItems.isEmpty()) return true;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            return commitOrRollback(conn,
                    () -> executeDecomposition(conn, orderId, allItems));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore scomposizione ordine", e);
            return false;
        }
    }

    private boolean executeDecomposition(Connection conn, int orderId,
                                         List<OrderItem> allItems) throws SQLException {
        String sqlRead = "SELECT tavolo, username, note FROM orders WHERE id = ?";
        Integer tavolo;
        String username;
        String note;

        try (PreparedStatement pstmt = conn.prepareStatement(sqlRead)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) return false;
                tavolo   = rs.getInt("tavolo");
                username = rs.getString("username");
                note     = rs.getString("note");
            }
        }

        Map<String, List<OrderItem>> itemsByCategory = new HashMap<>();
        for (OrderItem item : allItems) {
            String categoria = item.getProduct().getTipologia();
            itemsByCategory.computeIfAbsent(categoria, k -> new ArrayList<>()).add(item);
        }

        if (itemsByCategory.size() <= 1) return true;

        createCategoryOrders(conn, itemsByCategory, username, tavolo, note);
        deleteOriginalOrder(conn, orderId);
        return true;
    }

    private void deleteOriginalOrder(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement pstmtItems = conn.prepareStatement(
                "DELETE FROM order_items WHERE order_id = ?");
             PreparedStatement pstmtOrder = conn.prepareStatement(
                     "DELETE FROM orders WHERE id = ?")) {

            pstmtItems.setInt(1, orderId);
            pstmtItems.executeUpdate();
            pstmtOrder.setInt(1, orderId);
            pstmtOrder.executeUpdate();

            logger.log(Level.INFO, "Ordine originale #{0} eliminato dopo scomposizione", orderId);
        }
    }

    private void createCategoryOrders(Connection conn,
                                      Map<String, List<OrderItem>> itemsByCategory,
                                      String username, Integer tavolo,
                                      String note) throws SQLException {
        String sqlNewOrder = "INSERT INTO orders (username, tavolo, note, status) VALUES (?, ?, ?, ?)";
        String sqlNewItem  = "INSERT INTO order_items (order_id, menu_item_id, quantita, " +
                "prezzo_vendita_snapshot, costo_realizzazione_snapshot, nome_prodotto_snapshot) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmtNewOrder = conn.prepareStatement(
                sqlNewOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtNewItem = conn.prepareStatement(sqlNewItem)) {

            int ordersCreated = 0;
            for (List<OrderItem> categoryItems : itemsByCategory.values()) {
                if (categoryItems == null || categoryItems.isEmpty()) continue;

                int newOrderId = insertOrderAndGetId(pstmtNewOrder, username, tavolo, note);
                pstmtNewItem.setInt(1, newOrderId);

                for (OrderItem item : categoryItems) {
                    pstmtNewItem.setInt(2, item.getProduct().getId());
                    pstmtNewItem.setInt(3, item.getQuantita());
                    pstmtNewItem.setBigDecimal(4, item.getPrezzoSnapshot());
                    pstmtNewItem.setBigDecimal(5, item.getCostoSnapshot());
                    pstmtNewItem.setString(6, item.getNomeSnapshot());
                    pstmtNewItem.addBatch();
                }
                pstmtNewItem.executeBatch();
                pstmtNewItem.clearBatch();
                ordersCreated++;
            }
            logger.log(Level.INFO, "Creati {0} ordini separati per categoria", ordersCreated);
        }
    }

    private int insertOrderAndGetId(PreparedStatement pstmt, String username,
                                    Integer tavolo, String note) throws SQLException {
        pstmt.setString(1, username);
        if (tavolo != null) {
            pstmt.setInt(2, tavolo);
        } else {
            pstmt.setNull(2, Types.INTEGER);
        }
        pstmt.setString(3, note);
        pstmt.setString(4, TODOSTRING);
        pstmt.executeUpdate();

        try (ResultSet keys = pstmt.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("ID non generato per il nuovo ordine");
            return keys.getInt(1);
        }
    }

    // -------------------------------------------------------------------------
    // Tavoli e ordini pendenti
    // -------------------------------------------------------------------------

    @Override
    public boolean hasPendingOrders(int tavolo) {
        String sql = "SELECT EXISTS (SELECT 1 FROM orders WHERE tavolo = ? AND status != 'delivered')";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tavolo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo ordini pendenti", e);
            return false;
        }
    }

    @Override
    public List<Integer> getPendingOrderIds(int tavolo) {
        List<Integer> pendingIds = new ArrayList<>();
        String sql = "SELECT DISTINCT o.id FROM orders o " +
                "JOIN order_items oi ON o.id = oi.order_id " +
                "WHERE o.tavolo = ? AND o.status != 'delivered' AND o.status != 'canceled'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tavolo);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pendingIds.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero ID ordini pendenti", e);
        }
        return pendingIds;
    }

    @Override
    public Set<Integer> getTablesWithPendingOrders() {
        Set<Integer> tables = new HashSet<>();
        String sql = "SELECT DISTINCT tavolo FROM orders " +
                "WHERE status != 'delivered' AND status != 'closed'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tables.add(rs.getInt("tavolo"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero tavoli con ordini pendenti", e);
        }
        return tables;
    }

    // -------------------------------------------------------------------------
    // Statistiche
    // -------------------------------------------------------------------------

    @Override
    public long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COALESCE(SUM(oi.quantita), 0) " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE oi.menu_item_id = ? AND o.data_ora >= ? AND o.data_ora <= ? " +
                "AND o.status != 'canceled' AND oi.status <> 'canceled'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            pstmt.setTimestamp(2, Timestamp.valueOf(start));
            pstmt.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore conteggio vendite per data", e);
        }
        return 0;
    }
}