package com.example.rm.view.component;

import com.example.rm.controller.OrderUseCase;
import com.example.rm.model.Order;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.control.Separator;

import java.util.List;

public final class WaiterNotificationCardFactoryEink implements WaiterNotificationCardFactory {

    private final OrderUseCase orderUseCase;

    public WaiterNotificationCardFactoryEink(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @Override
    public Node createNotificationCard(Order order, Runnable onDelivered) {

        // Root: due colonne
        HBox root = new HBox(16);
        root.getStyleClass().addAll("eink-card", "eink-notification-card");
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.CENTER_LEFT);

        // Colonna sinistra: info
        VBox left = new VBox(8);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label title = new Label("TABLE: " + order.getTavolo());
        title.getStyleClass().addAll("eink-title");

        VBox contentBox = new VBox(6);
        contentBox.getStyleClass().add("eink-items");

        List<String> items = orderUseCase.loadOrderItemsForDisplay(order.getId());
        int limit = 3; // come ora
        for (int i = 0; i < items.size(); i++) {
            if (i >= limit) {
                Label more = new Label("+" + (items.size() - limit) + " more");
                more.getStyleClass().add("eink-more");
                contentBox.getChildren().add(more);
                break;
            }
            Label itemLbl = new Label(items.get(i));
            itemLbl.getStyleClass().add("eink-item");
            itemLbl.setWrapText(true);
            contentBox.getChildren().add(itemLbl);
        }

        left.getChildren().addAll(title, new Separator(), contentBox);

        // Colonna destra: bottone grande
        StackPane right = new StackPane();
        right.setAlignment(Pos.CENTER);
        right.setPrefWidth(160); // “colonna” dedicata
        right.setMinWidth(160);

        Button btnDone = new Button("DONE");
        btnDone.getStyleClass().addAll("eink-btn", "eink-btn-done");
        btnDone.setPrefSize(140, 140);
        btnDone.setMinSize(140, 140);
        btnDone.setMaxSize(140, 140);
        btnDone.setOnAction(e -> onDelivered.run());

        right.getChildren().add(btnDone);

        root.getChildren().addAll(left, right);

        VBox.setMargin(root, new Insets(0, 0, 14, 0)); // spazio sotto ogni card

        return root;
    }
}
