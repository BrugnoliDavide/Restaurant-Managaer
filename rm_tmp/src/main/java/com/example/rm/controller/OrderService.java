package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.service.DatabaseService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderService implements OrderUseCase {


    public static final Logger logger = Logger.getLogger(OrderService.class.getName());


    @Override
    public List<Order> loadAllOrders() {
        // usa già il calcolo del totale lato DB
        return DatabaseService.getAllOrdersWithTotal();
    }

    @Override
    public void markAsReady(int orderId) {
        // per la cucina/cameriere hai già ready/delivered
        DatabaseService.setOrderStatus(orderId, "ready");
    }

    @Override
    public void closeOrder(int orderId) {
        // chiudere = pagato
        DatabaseService.markOrderAsPaid(orderId);
    }

    @Override
    public void cancelOrder(int orderId) {
        DatabaseService.setOrderStatus(orderId, "canceled");
    }

    @Override
    public List<Order> loadOpenOrders() {
        // Ordini con status "to-do" o "ready"
        return DatabaseService.getOrdersByStatus("to-do"); // oppure adatta
    }

    @Override
    public boolean createOrder(
            List<OrderItem> items,
            Integer numeroTavolo,
            String note,
            com.example.rm.model.User user
    ) {
        return DatabaseService.createOrder(items, numeroTavolo, note, user);
    }

    @Override
    public List<Order> loadReadyOrdersForWaiter() {
        return DatabaseService.getReadyOrdersForWaiter();
    }

    @Override
    public List<String> loadOrderItemsForDisplay(int orderId) {
        return DatabaseService.getOrderItemsForDisplay(orderId);
    }

    @Override
    public boolean markOrderAsDelivered(int orderId) {
        return DatabaseService.markOrderAsDelivered(orderId);
    }

    @Override
    public Order getOrderById(int orderId) {
        if (orderId <= 0) {
            logger.log(Level.WARNING, "ID ordine non valido: {0}", orderId);
            return null;
        }

        try {
            // Recupera tutti gli ordini e filtra per ID
            List<Order> allOrders = DatabaseService.getAllOrdersWithTotal();

            return allOrders.stream()
                    .filter(order -> order.getId() == orderId)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore recupero ordine ID: {0}", orderId);
            return null;
        }
    }

    }
