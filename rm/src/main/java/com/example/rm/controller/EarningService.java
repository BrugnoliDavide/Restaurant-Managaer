package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.service.DatabaseService;

import java.util.List;

public class EarningService implements EarningUseCase {

    @Override
    public List<Order> loadOrdersToPay() {
        return DatabaseService.getOrdersToPay();
    }

    @Override
    public List<OrderItem> getOrderItemsDetailed(int orderId) {
        return DatabaseService.getOrderItemsDetailed(orderId);
    }

    @Override
    public List<Integer> getPendingOrderIds(int tavolo) {
        return DatabaseService.getPendingOrderIds(tavolo);
    }

    @Override
    public boolean hasPendingOrders(int tableNumber) {
        return DatabaseService.hasPendingOrders(tableNumber);
    }

    @Override
    public boolean markOrderAsPaid(int orderId) {
        return DatabaseService.markOrderAsPaid(orderId);
    }

    @Override
    public void setOrderStatus(int orderId, String status) {
        DatabaseService.setOrderStatus(orderId, status);
    }
}
