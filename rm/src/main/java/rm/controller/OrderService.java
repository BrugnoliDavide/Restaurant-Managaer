package rm.controller;

import rm.model.Order;
import rm.model.OrderItem;
import rm.model.User;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderService implements OrderUseCase {

    public static final Logger logger = Logger.getLogger(OrderService.class.getName());

    @Override
    public boolean createOrder(
            List<OrderItem> items,
            Integer numeroTavolo,
            String note,
            User user
    ) {
        return rm.service.OrderService.create(items, numeroTavolo, note, user);
    }

    @Override
    public List<Order> loadReadyOrdersForWaiter() {
        return rm.service.OrderService.getReadyForWaiter();
    }

    @Override
    public List<String> loadOrderItemsForDisplay(int orderId) {
        return rm.service.OrderService.getItemsForDisplay(orderId);
    }

    @Override
    public boolean markOrderAsDelivered(int orderId) {
        return rm.service.OrderService.markDelivered(orderId);
    }

    @Override
    public Order getOrderById(int orderId) {
        if (orderId <= 0) {
            logger.log(Level.WARNING, "ID ordine non valido: {0}", orderId);
            return null;
        }
        return rm.service.OrderService.findById(orderId);
    }

}