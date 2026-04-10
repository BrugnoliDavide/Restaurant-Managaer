package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.OrderService;

import java.util.List;

public class KitchenService implements KitchenUseCase {

    @Override
    public KitchenPreferences loadPreferences(String username) {
        return com.example.rm.service.KitchenService.getPreferences(username);
    }

    @Override
    public List<Order> loadFilteredOrders(String username) {
        return com.example.rm.service.KitchenService.getActiveOrdersFiltered(username);
    }

    @Override
    public List<Order> loadActiveOrders() {
        return OrderService.getKitchenActive();
    }

    @Override
    public void decomposeOrder(int orderId) {
        OrderService.decomposeIfNeeded(orderId);
    }

    @Override
    public void markOrderAsReady(int orderId) {
        OrderService.setStatus(orderId, "ready");
    }

    @Override
    public void splitMixedOrdersIfNeeded() {
        List<Order> allOrders = OrderService.getKitchenActive();
        for (Order order : allOrders) {
            OrderService.decomposeIfNeeded(order.getId());
        }
    }

    @Override
    public List<String> getOrderItemsDisplay(int orderId) {
        return OrderService.getItemsForDisplay(orderId);
    }

    @Override
    public boolean updateOrderStatus(int orderId, String status) {
        return OrderService.setStatus(orderId, status);
    }
}