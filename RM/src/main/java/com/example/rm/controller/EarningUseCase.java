package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;

import java.util.List;

public interface EarningUseCase {
    List<Order> loadOrdersToPay();
    List<OrderItem> getOrderItemsDetailed(int orderId);
    List<Integer> getPendingOrderIds(int tavolo);
    boolean hasPendingOrders(int tableNumber);
    boolean markOrderAsPaid(int orderId);
    void setOrderStatus(int orderId, String status);
}

