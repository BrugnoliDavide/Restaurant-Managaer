package rm.controller;

import rm.model.Order;
import rm.model.OrderItem;
import rm.model.User;

import java.util.List;

public interface OrderUseCase {


    List<Order> loadReadyOrdersForWaiter();

    List<String> loadOrderItemsForDisplay(int orderId);

    boolean markOrderAsDelivered(int orderId);


    boolean createOrder(
            List<OrderItem> items,
            Integer numeroTavolo,
            String note,
            User user
    );

    Order getOrderById(int orderId);
}
