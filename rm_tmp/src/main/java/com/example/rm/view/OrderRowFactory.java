package com.example.rm.view;

import com.example.rm.model.Order;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class OrderRowFactory {

    private OrderRowFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static HBox create(Order order, Consumer<Order> onSelect) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #DDD; -fx-border-radius: 10; -fx-cursor: hand;");

        // CORREZIONE: Uso di Insets invece di un intero
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        // Utilizzo dei dati dall'oggetto Order
        Label lblId = new Label("Ordine #" + order.getId());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label lblTable = new Label("Tavolo: " + order.getTavolo());
        info.getChildren().addAll(lblId, lblTable);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Formattazione del totale recuperato dal modello
        Label lblTotal = new Label(String.format("€ %.2f", order.getTotale()));
        lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2ecc71;");

        card.getChildren().addAll(info, spacer, lblTotal);

        // Gestione dell'azione al click tramite Consumer
        card.setOnMouseClicked(e -> {
            if (onSelect != null) {
                onSelect.accept(order);
            }
        });

        return card;
    }
}