package rm.view.component;

import rm.model.Order;
import javafx.scene.Node;

public interface KitchenOrderCardFactory {
    Node createOrderCard(Order order, Runnable onReady);
}
