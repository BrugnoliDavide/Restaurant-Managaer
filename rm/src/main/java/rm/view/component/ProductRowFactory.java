package rm.view.component;

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
        row.getStyleClass().add("product-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));

        row.setOnMouseClicked(e -> { if (onAdd != null) onAdd.run(); });

        VBox labels = new VBox(2);
        labels.getStyleClass().add("product-row-labels");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("product-row-title");
        labels.getChildren().add(lblTitle);

        if (subtitle != null && !subtitle.isBlank()) {
            Label lblSub = new Label(subtitle);
            lblSub.getStyleClass().add("product-row-subtitle");
            labels.getChildren().add(lblSub);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("product-row-value");

        HBox controls = new HBox(12);
        controls.getStyleClass().add("product-row-controls");
        controls.setAlignment(Pos.CENTER);
        controls.setOnMouseClicked(javafx.event.Event::consume);

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("product-row-minus");
        btnMinus.setMinWidth(30);
        btnMinus.setOnAction(e -> { if (onRemove != null) onRemove.run(); });

        Label lblQty = new Label(quantity > 0 ? String.valueOf(quantity) : "");
        lblQty.getStyleClass().add("product-row-qty");

        controls.getChildren().addAll(btnMinus, lblQty);
        row.getChildren().addAll(labels, spacer, lblValue, controls);
        return row;
    }


}