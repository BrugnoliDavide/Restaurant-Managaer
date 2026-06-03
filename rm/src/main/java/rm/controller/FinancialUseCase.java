package rm.controller;

import rm.model.Order;
import java.util.List;

public interface FinancialUseCase {
    List<Order> loadAllOrdersWithTotal();
    List<Order> loadAllOrdersWithDisplayItems();
}
