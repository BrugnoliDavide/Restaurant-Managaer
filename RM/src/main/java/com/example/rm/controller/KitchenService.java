package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;

import java.util.List;

public class KitchenService implements KitchenUseCase {

    @Override
    public KitchenPreferences loadPreferences(String username) {
        return DatabaseService.getKitchenPreferences(username);
    }

    @Override
    public List<Order> loadFilteredOrders(String username) {
        return DatabaseService.getKitchenActiveOrdersFiltered(username);
    }

    @Override
    public List<Order> loadActiveOrders() {
        return DatabaseService.getKitchenActiveOrders();
    }

    @Override
    public void decomposeOrder(int orderId) {
        DatabaseService.decomposeOrderIfNeeded(orderId);
    }

    @Override
    public void markOrderAsReady(int orderId) {
        DatabaseService.setOrderStatus(orderId, "ready");
    }

    @Override
    public void splitMixedOrdersIfNeeded() {
        List<Order> allOrders = DatabaseService.getKitchenActiveOrders();
        for (Order order : allOrders) {
            DatabaseService.decomposeOrderIfNeeded(order.getId());
        }
    }

    @Override
    public List<String> getOrderItemsDisplay(int orderId) {
        return DatabaseService.getOrderItemsForDisplay(orderId);
    }


}
