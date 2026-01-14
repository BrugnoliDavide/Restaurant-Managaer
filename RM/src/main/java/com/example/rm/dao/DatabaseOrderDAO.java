package com.example.rm.dao;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import java.util.List;

public class DatabaseOrderDAO implements OrderDAO {
    @Override
    public List<Order> loadAllOrdersWithDisplayItems() {
        // Usa getAllOrdersWithTotal() + batch getOrderItemsForDisplay
        List<Order> orders = DatabaseService.getAllOrdersWithTotal();
        //volendo si potrebbe fare un JOIN SQL in 1 query o cache Map<Integer, List<String>> per migliorare le prestazioni

        return orders;
    }

    @Override
    public List<String> getOrderItemsForDisplay(int orderId) {
        return DatabaseService.getOrderItemsForDisplay(orderId);
    }
}
