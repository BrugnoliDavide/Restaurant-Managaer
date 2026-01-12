package com.example.rm.controller;

import com.example.rm.model.Order;

import java.util.List;

public interface ManagerUseCase {


    List<Order> loadAllOrders();

    boolean updateOrderStatus(int orderId, String newStatus);


    boolean deleteOrder(int orderId);


    Order findOrderById(int orderId);
}
