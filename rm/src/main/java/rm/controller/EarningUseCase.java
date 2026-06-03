package rm.controller;

import rm.model.Order;
import rm.model.OrderItem;

import java.util.List;

public interface EarningUseCase {
    List<Order> loadOrdersToPay();
    List<OrderItem> getOrderItemsDetailed(int orderId);
    List<Integer> getPendingOrderIds(int tavolo);
    boolean hasPendingOrders(int tableNumber);
    boolean markOrderAsPaid(int orderId);
    void setOrderStatus(int orderId, String status);
    void cancelItemFromOrder(int orderId, int itemId);
}

