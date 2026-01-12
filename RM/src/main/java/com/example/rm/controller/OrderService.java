package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;

import java.util.List;

public class OrderService implements OrderUseCase {

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

}
