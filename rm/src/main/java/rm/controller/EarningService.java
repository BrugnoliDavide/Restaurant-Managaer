package rm.controller;

import rm.model.Order;
import rm.model.OrderItem;
import rm.service.OrderService;

import java.util.List;

public class EarningService implements EarningUseCase {

    @Override
    public List<Order> loadOrdersToPay() {
        return OrderService.getToPay();
    }

    @Override
    public List<OrderItem> getOrderItemsDetailed(int orderId) {
        return OrderService.getItemsDetailed(orderId);
    }

    @Override
    public List<Integer> getPendingOrderIds(int tavolo) {
        return OrderService.getPendingOrderIds(tavolo);
    }

    @Override
    public boolean hasPendingOrders(int tableNumber) {
        return OrderService.hasPendingOrders(tableNumber);
    }

    @Override
    public boolean markOrderAsPaid(int orderId) {
        return OrderService.markPaid(orderId);
    }

    @Override
    public void setOrderStatus(int orderId, String status) {
        OrderService.setStatus(orderId, status);
    }

    @Override
    public void cancelItemFromOrder(int orderId, int itemId) {
        OrderService.removeOrderItem(orderId, itemId);
    }
}
