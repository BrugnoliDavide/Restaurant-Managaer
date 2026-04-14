package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderService implements OrderUseCase {

    public static final Logger logger = Logger.getLogger(OrderService.class.getName());

    @Override
    public boolean createOrder(
            List<OrderItem> items,
            Integer numeroTavolo,
            String note,
            com.example.rm.model.User user
    ) {
        return com.example.rm.service.OrderService.create(items, numeroTavolo, note, user);
    }

    @Override
    public List<Order> loadReadyOrdersForWaiter() {
        return com.example.rm.service.OrderService.getReadyForWaiter();
    }

    @Override
    public List<String> loadOrderItemsForDisplay(int orderId) {
        return com.example.rm.service.OrderService.getItemsForDisplay(orderId);
    }

    @Override
    public boolean markOrderAsDelivered(int orderId) {
        return com.example.rm.service.OrderService.markDelivered(orderId);
    }

    @Override
    public Order getOrderById(int orderId) {
        if (orderId <= 0) {
            logger.log(Level.WARNING, "ID ordine non valido: {0}", orderId);
            return null;
        }

        try {
            List<Order> allOrders = com.example.rm.service.OrderService.getAllWithTotal();

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