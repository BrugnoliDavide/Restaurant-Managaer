package com.example.rm.service;

import com.example.rm.dao.OrderDAO;
import com.example.rm.dao.impl.OrderDAOFile;
import com.example.rm.dao.impl.OrderDAOPostgres;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.util.BeanMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade sul layer DAO per la gestione degli ordini.
 *
 * <p>Questo è il <strong>punto di mappatura</strong> Bean → Model:
 * i DAO restituiscono Bean, {@code BeanMapper} li converte in Model
 * prima che raggiungano Controller e View.</p>
 *
 * <p>I Controller non devono mai importare classi del package {@code bean}.</p>
 */
public final class OrderService {

    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    private static final AtomicReference<OrderDAO> dao = new AtomicReference<>(null);

    private OrderService() {
        throw new IllegalStateException("Utility class");
    }

    // -------------------------------------------------------------------------
    // Inizializzazione DAO
    // -------------------------------------------------------------------------

    /** Attiva l'implementazione PostgreSQL. Da invocare dopo {@link ConnectionManager#configure}. */
    public static synchronized void usePostgres() {
        dao.set(new OrderDAOPostgres());
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
            dao.set(new OrderDAOFile(basePath));
            logger.log(Level.INFO, "OrderDAO: modalità file system attiva: {0}", basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile inizializzare file system", e);
        }
    }

    private static OrderDAO dao() {
        OrderDAO current = dao.get();
        if (current == null) {
            throw new IllegalStateException(
                    "OrderService non inizializzato. Invocare usePostgres() o useFileSystem().");
        }
        return current;
    }

    // -------------------------------------------------------------------------
    // Operazioni di scrittura — i parametri restano Model (provengono dalla UI)
    // -------------------------------------------------------------------------

    public static boolean create(List<OrderItem> items, Integer tavolo,
                                 String note, User utente) {
        return dao().createOrder(items, tavolo, note, utente);
    }

    public static boolean setStatus(int orderId, String newStatus) {
        return dao().setOrderStatus(orderId, newStatus);
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

    public static boolean removeOrderItem(int orderId, int itemId) {
        return dao().removeOrderItem(orderId, itemId);
    }

    // -------------------------------------------------------------------------
    // Operazioni di lettura — DAO restituisce Bean, qui mappiamo a Model
    // -------------------------------------------------------------------------

    /** Tutti gli ordini con totale. */
    public static List<Order> getAllWithTotal() {
        return BeanMapper.toOrderModels(dao().getAllOrdersWithTotal());
    }

    /** Ordini filtrati per stato. */
    public static List<Order> getByStatus(String status) {
        return BeanMapper.toOrderModels(dao().getOrdersByStatus(status));
    }

    /** Ordini attivi in cucina (ultime 24 ore). */
    public static List<Order> getKitchenActive() {
        return BeanMapper.toOrderModels(dao().getKitchenActiveOrders());
    }

    /** Ordini pronti per il cameriere. */
    public static List<Order> getReadyForWaiter() {
        return BeanMapper.toOrderModels(dao().getReadyOrdersForWaiter());
    }

    /** Ordini da pagare alla cassa. */
    public static List<Order> getToPay() {
        return BeanMapper.toOrderModels(dao().getOrdersToPay());
    }

    /**
     * Cerca un ordine per ID.
     *
     * @return l'ordine oppure {@code null} se non trovato
     */
    public static Order findById(int orderId) {
        return BeanMapper.toModel(dao().findById(orderId));
    }

    /**
     * Dettagli articoli di un ordine (con dati di snapshot completi).
     * La tipologia del prodotto è vuota: questo metodo è pensato per la
     * visualizzazione, non per la decomposizione per categoria.
     */
    public static List<OrderItem> getItemsDetailed(int orderId) {
        return BeanMapper.toOrderItemModels(dao().getOrderItemsDetailed(orderId));
    }

    /** Articoli formattati per la visualizzazione ("Qtà x NomeProdotto"). */
    public static List<String> getItemsForDisplay(int orderId) {
        return dao().getOrderItemsForDisplay(orderId);
    }

    /** Mappa orderId → stringhe, per il caricamento massivo nella vista Financial. */
    public static Map<Integer, List<String>> getAllItemsForDisplay() {
        return dao().getAllOrderItemsForDisplay();
    }

    // -------------------------------------------------------------------------
    // Utilità
    // -------------------------------------------------------------------------

    public static boolean hasPendingOrders(int tavolo) {
        return dao().hasPendingOrders(tavolo);
    }

    public static List<Integer> getPendingOrderIds(int tavolo) {
        return dao().getPendingOrderIds(tavolo);
    }

    public static Set<Integer> getTablesWithPendingOrders() {
        return dao().getTablesWithPendingOrders();
    }

    public static long getQuantitySoldInRange(int productId,
                                              LocalDateTime start,
                                              LocalDateTime end) {
        return dao().getQuantitySoldInDateRange(productId, start, end);
    }
}