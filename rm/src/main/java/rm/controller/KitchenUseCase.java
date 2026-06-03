package rm.controller;

import rm.model.Order;
import rm.preference.KitchenPreferences;

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
