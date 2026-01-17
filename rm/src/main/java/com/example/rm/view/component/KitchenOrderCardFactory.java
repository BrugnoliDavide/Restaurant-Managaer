package com.example.rm.view.component;

import com.example.rm.model.Order;
import javafx.scene.Node;

public interface KitchenOrderCardFactory {
    Node createOrderCard(Order order, Runnable onReady);
}
