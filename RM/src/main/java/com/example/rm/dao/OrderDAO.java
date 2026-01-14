package com.example.rm.dao;

import com.example.rm.model.Order;
import java.util.List;

public interface OrderDAO {
    List<Order> loadAllOrdersWithDisplayItems();
    List<String> getOrderItemsForDisplay(int orderId);
}
