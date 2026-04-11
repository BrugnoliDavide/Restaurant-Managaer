package com.example.rm.controller;
import com.example.rm.model.Order;
import com.example.rm.service.ConnectionManager;
import com.example.rm.service.LoggerService;
import com.example.rm.service.OrderService;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;




public class FinancialService implements FinancialUseCase {
    private static final Logger logger = LoggerService.getLogger(FinancialService.class);


    @Override
    public List<Order> loadAllOrdersWithTotal() {
        return OrderService.getAllWithTotal();
    }

    @Override
    public List<Order> loadAllOrdersWithDisplayItems() {
        List<Order> orders = OrderService.getAllWithTotal();
        Map<Integer, List<String>> allItems = OrderService.getAllItemsForDisplay();

        for (Order order : orders) {
            List<String> items = allItems.getOrDefault(order.getId(), Collections.emptyList());
            order.setDisplayItems(items);
        }

        logger.log(Level.INFO, "Precaricati items per {0} ordini", orders.size());
        return orders;
    }

}
