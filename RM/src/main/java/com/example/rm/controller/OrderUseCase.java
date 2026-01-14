package com.example.rm.controller;

import com.example.rm.model.Order;
import java.util.List;

public interface OrderUseCase {

    List<Order> loadAllOrders();

    List<Order> loadOpenOrders();

    List<Order> loadReadyOrdersForWaiter();

    List<String> loadOrderItemsForDisplay(int orderId);

    boolean markOrderAsDelivered(int orderId);

    void markAsReady(int orderId);

    void closeOrder(int orderId);

    void cancelOrder(int orderId);

    boolean createOrder(
            List<com.example.rm.model.OrderItem> items,
            Integer numeroTavolo,
            String note,
            com.example.rm.model.User user
    );

    Order getOrderById(int orderId);
}
