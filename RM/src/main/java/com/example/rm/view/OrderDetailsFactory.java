package com.example.rm.view;

import com.example.rm.model.Order;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class OrderDetailsFactory {

    // CORRETTO: Costruttore privato per utility class
    private OrderDetailsFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static VBox create(Order order,
                              java.util.function.Consumer<Double> onPay) {

        VBox root = new VBox(10);


        Label total = new Label(String.format("Totale: €%.2f", order.getTotale()));

        TextField discountField = new TextField();
        discountField.setPromptText("Discount");

        Button pay = new Button("Mark as paid");

        pay.setOnAction(e -> {
            // PROBLEMA POTENZIALE: NumberFormatException non gestita
            try {
                double discount = discountField.getText().isEmpty()
                        ? 0
                        : Double.parseDouble(discountField.getText());
                onPay.accept(discount);
            } catch (NumberFormatException ex) {
                // TODO: Mostrare errore all'utente
                discountField.setStyle("-fx-border-color: red;");
            }
        });

        root.getChildren().addAll(total, discountField, pay);
        return root;
    }
}