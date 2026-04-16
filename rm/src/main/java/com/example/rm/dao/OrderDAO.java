package com.example.rm.dao;

import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Access Object per la gestione degli ordini.
 * Fornisce un'astrazione per l'accesso ai dati indipendente dall'implementazione.
 */
public interface OrderDAO {

    /**
     * Crea un nuovo ordine con i relativi articoli
     * @param items Lista articoli da ordinare
     * @param tavolo Numero tavolo (può essere null)
     * @param note Note aggiuntive (può essere null)
     * @param utente Utente che crea l'ordine
     * @return true se l'ordine è stato creato con successo
     */
    boolean createOrder(List<OrderItem> items, Integer tavolo, String note, User utente);

    /**
     * Recupera tutti gli ordini con il totale calcolato
     * @return Lista di ordini
     */
    List<Order> getAllOrdersWithTotal();

    /**
     * Recupera gli ordini filtrati per stato
     * @param status Stato target
     * @return Lista di ordini con lo stato specificato
     */
    List<Order> getOrdersByStatus(String status);

    /**
     * Recupera gli ordini attivi per la cucina (ultime 24 ore)
     * @return Lista di ordini attivi
     */
    List<Order> getKitchenActiveOrders();

    /**
     * Modifica lo stato di un ordine
     * @param orderId ID dell'ordine
     * @param newStatus Nuovo stato
     * @return true se l'aggiornamento è riuscito
     */
    boolean setOrderStatus(int orderId, String newStatus);

    /**
     * Recupera i dettagli degli articoli di un ordine per la visualizzazione
     * @param orderId ID dell'ordine
     * @return Lista di stringhe formattate "Qtà x Nome Prodotto"
     */
    List<String> getOrderItemsForDisplay(int orderId);

    /**
     * Recupera i dettagli completi degli articoli di un ordine
     * @param orderId ID dell'ordine
     * @return Lista di OrderItem con tutti i dati
     */
    List<OrderItem> getOrderItemsDetailed(int orderId);

    /**
     * Recupera gli ordini pronti per il cameriere
     * @return Lista di ordini con stato 'ready'
     */
    List<Order> getReadyOrdersForWaiter();

    /**
     * Recupera gli ordini da pagare alla cassa
     * @return Lista di ordini con stato 'delivered'
     */
    List<Order> getOrdersToPay();

    /**
     * Marca un ordine come consegnato
     * @param orderId ID dell'ordine
     * @return true se l'operazione è riuscita
     */
    boolean markOrderAsDelivered(int orderId);

    /**
     * Marca un ordine come pagato
     * @param orderId ID dell'ordine
     * @return true se l'operazione è riuscita
     */
    boolean markOrderAsPaid(int orderId);

    /**
     * Scompone un ordine in più ordini per categoria se necessario
     * @param orderId ID dell'ordine da scomporre
     * @return true se l'operazione è riuscita
     */
    boolean decomposeOrderIfNeeded(int orderId);

    /**
     * Verifica se un tavolo ha ordini pendenti
     * @param tavolo Numero tavolo
     * @return true se esistono ordini non consegnati
     */
    boolean hasPendingOrders(int tavolo);

    /**
     * Recupera gli ID degli ordini pendenti per un tavolo
     * @param tavolo Numero tavolo
     * @return Lista di ID ordini pendenti
     */
    List<Integer> getPendingOrderIds(int tavolo);

    /**
     * Recupera la quantità venduta di un prodotto in un intervallo di date
     * @param productId ID del prodotto
     * @param start Data inizio
     * @param end Data fine
     * @return Quantità totale venduta
     */
    long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end);


    Map<Integer, List<String>> getAllOrderItemsForDisplay();

    Set<Integer> getTablesWithPendingOrders();

    Order findById(int orderId);

    boolean removeOrderItem(int orderId, int itemId);
}