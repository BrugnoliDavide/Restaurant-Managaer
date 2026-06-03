package rm.view.component;

import rm.controller.OrderUseCase;
import rm.model.Order;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.List;

public final class WaiterNotificationCardFactoryClassic implements WaiterNotificationCardFactory {

    private final OrderUseCase orderUseCase;

    public WaiterNotificationCardFactoryClassic(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @Override
    public Node createNotificationCard(Order order, Runnable onDelivered) {
        VBox card = new VBox(10);
        card.getStyleClass().add("notification-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(6);
        dot.getStyleClass().add("notification-dot");

        Circle glow = new Circle(10);
        glow.getStyleClass().add("notification-glow");

        StackPane indicator = new StackPane(glow, dot);

        Label title = new Label("To deliver: table " + order.getTavolo());
        title.getStyleClass().add("notification-title");

        header.getChildren().addAll(indicator, title);

        VBox contentBox = new VBox(2);
        List<String> items = orderUseCase.loadOrderItemsForDisplay(order.getId());

        int limit = 3;
        for (int i = 0; i < items.size(); i++) {
            if (i >= limit) {
                Label more = new Label("... (+" + (items.size() - limit) + " altri)");
                more.getStyleClass().add("notification-more");
                contentBox.getChildren().add(more);
                break;
            }

            Label itemLbl = new Label(items.get(i));
            itemLbl.getStyleClass().add("notification-item");
            itemLbl.setWrapText(true);
            contentBox.getChildren().add(itemLbl);
        }

        Button btnDelivered = new Button("Delivered");
        btnDelivered.setMaxWidth(Double.MAX_VALUE);
        btnDelivered.getStyleClass().add("btn-delivered");
        btnDelivered.setOnAction(e -> onDelivered.run());

        card.getChildren().addAll(header, contentBox, new Separator(), btnDelivered);
        return card;
    }
}
