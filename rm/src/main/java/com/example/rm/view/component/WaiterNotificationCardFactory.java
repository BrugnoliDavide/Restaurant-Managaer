package com.example.rm.view.component;

import com.example.rm.model.Order;
import javafx.scene.Node;

public interface WaiterNotificationCardFactory {
    Node createNotificationCard(Order order, Runnable onDelivered);
}
