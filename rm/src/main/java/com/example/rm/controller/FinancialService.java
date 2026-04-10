package com.example.rm.controller;


import com.example.rm.model.Order;
import com.example.rm.service.LoggerService;
import com.example.rm.service.OrderService;

import java.util.List;
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
        for (Order order : orders) {
            order.getDisplayItems();
        }
        logger.log(Level.INFO,"Precaricati items per {0} ordini", orders.size());
        return orders;
    }
}
