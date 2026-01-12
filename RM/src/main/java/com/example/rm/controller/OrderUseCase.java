package com.example.rm.controller;

import com.example.rm.model.Order;
import java.util.List;

public interface OrderUseCase {

    List<Order> loadAllOrders();

    List<Order> loadOpenOrders();

    void markAsReady(int orderId);

    void closeOrder(int orderId);

    void cancelOrder(int orderId);
}
