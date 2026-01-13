package com.example.rm.controller;

import com.example.rm.model.Order;
import java.util.List;

public interface FinancialUseCase {
    List<Order> loadAllOrdersWithTotal();
}
