package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;

import java.util.List;

public class FinancialService implements FinancialUseCase {

    @Override
    public List<Order> loadAllOrdersWithTotal() {
        return DatabaseService.getAllOrdersWithTotal();
    }
}
