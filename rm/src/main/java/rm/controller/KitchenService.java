package rm.controller;

import rm.model.Order;
import rm.preference.KitchenPreferences;
import rm.service.OrderService;

import java.util.List;

public class KitchenService implements KitchenUseCase {

    @Override
    public KitchenPreferences loadPreferences(String username) {
        return rm.service.KitchenService.getPreferences(username);
    }

    @Override
    public List<Order> loadFilteredOrders(String username) {
        return rm.service.KitchenService.getActiveOrdersFiltered(username);
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