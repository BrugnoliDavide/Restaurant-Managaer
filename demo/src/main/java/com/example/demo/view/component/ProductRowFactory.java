package com.example.demo.view.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class ProductRowFactory {

    private ProductRowFactory() {}

    public static HBox row(String title, String subtitle, String value, int quantity, Runnable onAdd, Runnable onRemove) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));

        // Stile base e hover per la riga intera (che ora funge da tasto "Aggiungi")
        String baseStyle = "-fx-background-color: white; -fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #F9F9F9; -fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        row.setStyle(baseStyle);

        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e -> row.setStyle(baseStyle));

        // CLICK SULLA RIGA -> Incrementa (onAdd)
        row.setOnMouseClicked(e -> {
            if (onAdd != null) onAdd.run();
        });

        // 1. Label Informative
        VBox labels = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        labels.getChildren().add(lblTitle);

        if (subtitle != null && !subtitle.isBlank()) {
            Label lblSub = new Label(subtitle);
            lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
            labels.getChildren().add(lblSub);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. Prezzo
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        // 3. Area Controlli (Zona protetta)
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);
        // Fondamentale: Impediamo che il click sul tasto MENO si propaghi alla riga (evita +1 e -1 simultanei)
        controls.setOnMouseClicked(e -> e.consume());

        // Tasto MENO (Decrementa)
        Button btnMinus = new Button("-");
        btnMinus.setMinWidth(30);
        btnMinus.setStyle("-fx-background-color: #FFE5E5; -fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        btnMinus.setOnAction(e -> {
            if (onRemove != null) onRemove.run();
        });

        // Quantità
        Label lblQty = new Label(quantity > 0 ? String.valueOf(quantity) : "");
        lblQty.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333; -fx-min-width: 20; -fx-alignment: center;");

        controls.getChildren().addAll(btnMinus, lblQty);

        row.getChildren().addAll(labels, spacer, lblValue, controls);
        return row;
    }
}