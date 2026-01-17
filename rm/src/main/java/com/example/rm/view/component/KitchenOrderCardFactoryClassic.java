package com.example.rm.view.component;

import com.example.rm.controller.KitchenUseCase;
import com.example.rm.model.Order;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class KitchenOrderCardFactoryClassic implements KitchenOrderCardFactory {

    private final KitchenUseCase kitchenUseCase;

    public KitchenOrderCardFactoryClassic(KitchenUseCase kitchenUseCase) {
        this.kitchenUseCase = kitchenUseCase;
    }

    @Override
    public Node createOrderCard(Order order, Runnable onReady) {
        HBox card = new HBox(20);
        card.getStyleClass().add("order-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox leftInfo = new VBox(5);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        String titleText = "Ordine #" + order.getId();
        if (order.getTavolo() > 0) titleText += " (Tavolo " + order.getTavolo() + ")";

        Label lblTitle = new Label(titleText);
        lblTitle.getStyleClass().add("order-title");

        Label lblTime = new Label("Arrivato alle: " +
                order.getDataOra().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.getStyleClass().add("order-time");

        VBox itemsBox = new VBox(2);
        itemsBox.setPadding(new Insets(10, 0, 0, 0));

        List<String> items = kitchenUseCase.getOrderItemsDisplay(order.getId());
        for (String itemStr : items) {
            Label itemLbl = new Label("• " + itemStr);
            itemLbl.getStyleClass().add("order-item");
            itemsBox.getChildren().add(itemLbl);
        }

        leftInfo.getChildren().addAll(lblTitle, lblTime, itemsBox);

        if (order.getNote() != null && !order.getNote().isEmpty()) {
            Label lblNote = new Label("NOTE: " + order.getNote());
            lblNote.getStyleClass().add("order-note-kitchen");
            leftInfo.getChildren().add(lblNote);
        }

        Button btnDone = new Button("PRONTO");
        btnDone.getStyleClass().add("btn-ready");
        btnDone.setOnAction(e -> onReady.run());

        card.getChildren().addAll(leftInfo, btnDone);
        return card;
    }
}
