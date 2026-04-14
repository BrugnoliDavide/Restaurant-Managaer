package com.example.rm.service;

import com.example.rm.dao.OrderDAO;
import com.example.rm.dao.impl.OrderDAOFile;
import com.example.rm.dao.impl.OrderDAOPostgres;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestisce l'accesso agli ordini attraverso il DAO attivo.
 *
 * <p>La scelta dell'implementazione (Postgres o FileSystem) è responsabilità
 * esclusiva di questa classe. Il campo {@code volatile} garantisce che ogni
 * thread veda l'ultima implementazione assegnata.</p>
 */
public final class OrderService {

    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    private static volatile OrderDAO dao = null;



    private OrderService() {
        throw new IllegalStateException("Utility class");
    }

    // -------------------------------------------------------------------------
    // Inizializzazione DAO
    // -------------------------------------------------------------------------

    /**
     * Attiva l'implementazione PostgreSQL.
     * Da invocare dopo {@link ConnectionManager#configure}.
     */
    public static synchronized void usePostgres() {
        dao = new OrderDAOPostgres();
        logger.info("OrderDAO: modalità PostgreSQL attiva");
    }

    /**
     * Attiva l'implementazione su file system.
     *
     * @param basePath percorso base per i file degli ordini
     * @throws IllegalStateException se l'inizializzazione fallisce
     */
    public static synchronized void useFileSystem(String basePath) {
        try {
            dao = new OrderDAOFile(basePath);
            logger.log(Level.INFO, "OrderDAO: modalità file system attiva: {0}", basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile inizializzare file system", e);
        }
    }

    // -------------------------------------------------------------------------
    // Accesso al DAO
    // -------------------------------------------------------------------------

    private static OrderDAO dao() {
        OrderDAO current = dao;
        if (current == null) {
            throw new IllegalStateException(
                    "OrderService non inizializzato. Invocare usePostgres() o useFileSystem().");
        }
        return current;
    }

    // -------------------------------------------------------------------------
    // Operazioni sugli ordini
    // -------------------------------------------------------------------------

    public static boolean create(List<OrderItem> items, Integer tavolo,
                                 String note, User utente) {
        return dao().createOrder(items, tavolo, note, utente);
    }

    public static boolean setStatus(int orderId, String newStatus) {
        return dao().setOrderStatus(orderId, newStatus);
    }

    public static List<Order> getAllWithTotal() {
        return dao().getAllOrdersWithTotal();
    }

    public static List<Order> getByStatus(String status) {
        return dao().getOrdersByStatus(status);
    }

    public static List<Order> getKitchenActive() {
        return dao().getKitchenActiveOrders();
    }

    public static List<String> getItemsForDisplay(int orderId) {
        return dao().getOrderItemsForDisplay(orderId);
    }

    public static List<OrderItem> getItemsDetailed(int orderId) {
        return dao().getOrderItemsDetailed(orderId);
    }

    public static List<Order> getReadyForWaiter() {
        return dao().getReadyOrdersForWaiter();
    }

    public static List<Order> getToPay() {
        return dao().getOrdersToPay();
    }

    public static boolean markDelivered(int orderId) {
        return dao().markOrderAsDelivered(orderId);
    }

    public static boolean markPaid(int orderId) {
        return dao().markOrderAsPaid(orderId);
    }

    public static boolean decomposeIfNeeded(int orderId) {
        return dao().decomposeOrderIfNeeded(orderId);
    }

    public static boolean hasPendingOrders(int tavolo) {
        return dao().hasPendingOrders(tavolo);
    }

    public static List<Integer> getPendingOrderIds(int tavolo) {
        return dao().getPendingOrderIds(tavolo);
    }

    public static long getQuantitySoldInRange(int productId,
                                              LocalDateTime start,
                                              LocalDateTime end) {
        return dao().getQuantitySoldInDateRange(productId, start, end);
    }

    public static Map<Integer, List<String>> getAllItemsForDisplay() {
        return dao().getAllOrderItemsForDisplay();
    }

    public static Set<Integer> getTablesWithPendingOrders() {
        return dao().getTablesWithPendingOrders();
    }

    public static Order findById(int orderId) {
        return dao().findById(orderId);
    }
}