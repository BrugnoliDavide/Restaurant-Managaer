package com.example.rm.view.component;

import com.example.rm.controller.KitchenUseCase;
import com.example.rm.model.Order;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class KitchenOrderCardFactoryEink implements KitchenOrderCardFactory {

    private final KitchenUseCase kitchenUseCase;

    public KitchenOrderCardFactoryEink(KitchenUseCase kitchenUseCase) {
        this.kitchenUseCase = kitchenUseCase;
    }

    @Override
    public Node createOrderCard(Order order, Runnable onReady) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("eink-card", "eink-order-card");
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.CENTER_LEFT);

        String header = (order.getTavolo() > 0)
                ? ("TAVOLO " + order.getTavolo())
                : ("ORDINE " + order.getId());

        Label lblTitle = new Label(header);
        lblTitle.getStyleClass().addAll("eink-title");

        Label lblTime = new Label(order.getDataOra().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.getStyleClass().addAll("eink-subtitle");

        VBox itemsBox = new VBox(6);
        itemsBox.getStyleClass().add("eink-items");

        List<String> items = kitchenUseCase.getOrderItemsDisplay(order.getId());
        int limit = 5;
        for (int i = 0; i < items.size() && i < limit; i++) {
            Label itemLbl = new Label(items.get(i));
            itemLbl.getStyleClass().add("eink-item");
            itemsBox.getChildren().add(itemLbl);
        }
        if (items.size() > limit) {
            Label more = new Label("+" + (items.size() - limit) + " altri");
            more.getStyleClass().add("eink-more");
            itemsBox.getChildren().add(more);
        }

        Button btnDone = new Button("PRONTO");
        btnDone.getStyleClass().addAll("eink-btn", "eink-btn-primary");
        btnDone.setOnAction(e -> onReady.run());

        card.getChildren().addAll(lblTitle, lblTime, itemsBox, btnDone);
        return card;
    }
}
