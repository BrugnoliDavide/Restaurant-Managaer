package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.preference.KitchenPreferences;

import java.util.List;

public interface KitchenUseCase {
    KitchenPreferences loadPreferences(String username);
    List<Order> loadFilteredOrders(String username);
    List<Order> loadActiveOrders();
    void decomposeOrder(int orderId);
    void markOrderAsReady(int orderId);
    void splitMixedOrdersIfNeeded();
    List<String> getOrderItemsDisplay(int orderId);
    boolean updateOrderStatus(int orderId, String status);
}
