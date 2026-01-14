package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.model.User;

import java.util.List;

public interface ManagerUseCase {


    List<Order> loadAllOrders();

    boolean updateOrderStatus(int orderId, String newStatus);

    boolean deleteOrder(int orderId);

    Order findOrderById(int orderId);

    List<User> loadAllUsers();

    boolean deleteUser(String userId);

}
