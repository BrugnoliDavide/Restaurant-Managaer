package rm.view.component;

import rm.model.Order;
import javafx.scene.Node;

public interface WaiterNotificationCardFactory {
    Node createNotificationCard(Order order, Runnable onDelivered);
}
