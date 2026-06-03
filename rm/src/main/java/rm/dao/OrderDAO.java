package rm.dao;

import rm.bean.OrderBean;
import rm.bean.OrderItemBean;
import rm.model.OrderItem;
import rm.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Access Object per la gestione degli ordini.
 *
 * <p>Tutti i metodi di <em>lettura</em> restituiscono Bean (puro Java, senza
 * dipendenze JavaFX). Il mapping a Model avviene nel layer Service tramite
 * {@code BeanMapper}.</p>
 *
 * <p>I metodi di <em>scrittura</em> continuano ad accettare i Model
 * ({@code OrderItem}, {@code User}) poiché arrivano direttamente dalla UI.</p>
 */
public interface OrderDAO {

    // -------------------------------------------------------------------------
    // Scrittura — parametri restano Model (provengono dalla UI)
    // -------------------------------------------------------------------------

    /**
     * Crea un nuovo ordine con i relativi articoli.
     */
    boolean createOrder(List<OrderItem> items, Integer tavolo, String note, User utente);

    /**
     * Modifica lo stato di un ordine.
     */
    boolean setOrderStatus(int orderId, String newStatus);

    boolean markOrderAsDelivered(int orderId);

    boolean markOrderAsPaid(int orderId);

    boolean decomposeOrderIfNeeded(int orderId);

    boolean hasPendingOrders(int tavolo);

    boolean removeOrderItem(int orderId, int itemId);

    // -------------------------------------------------------------------------
    // Lettura ordini — restituisce OrderBean
    // -------------------------------------------------------------------------

    /**
     * Tutti gli ordini con totale calcolato, ordinati per data decrescente.
     */
    List<OrderBean> getAllOrdersWithTotal();

    /**
     * Ordini filtrati per stato.
     */
    List<OrderBean> getOrdersByStatus(String status);

    /**
     * Ordini attivi per la cucina (ultime 24 ore, stato "to-do").
     */
    List<OrderBean> getKitchenActiveOrders();

    /**
     * Ordini pronti per il cameriere (stato "ready").
     */
    List<OrderBean> getReadyOrdersForWaiter();

    /**
     * Ordini da pagare alla cassa (stato "delivered").
     */
    List<OrderBean> getOrdersToPay();

    /**
     * Cerca un singolo ordine per ID.
     *
     * @return {@code OrderBean} oppure {@code null} se non trovato
     */
    OrderBean findById(int orderId);

    // -------------------------------------------------------------------------
    // Lettura articoli — restituisce OrderItemBean
    // -------------------------------------------------------------------------

    /**
     * Dettagli completi degli articoli di un ordine.
     */
    List<OrderItemBean> getOrderItemsDetailed(int orderId);

    /**
     * Articoli formattati per la visualizzazione ("Qtà x NomeProdotto").
     * Restituisce stringhe, non ha senso mappare a Bean.
     */
    List<String> getOrderItemsForDisplay(int orderId);

    /**
     * Mappa orderId → lista stringhe per display, usata nel caricamento massivo.
     */
    Map<Integer, List<String>> getAllOrderItemsForDisplay();

    // -------------------------------------------------------------------------
    // Utilità
    // -------------------------------------------------------------------------

    List<Integer> getPendingOrderIds(int tavolo);

    Set<Integer> getTablesWithPendingOrders();

    long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end);
}