package com.example.RM.view.component;

import com.example.RM.model.Order;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import static com.example.RM.view.LoginController.logger;

public class OrderRowController {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private HBox root;

    private Order order;

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm");

    public void setOrder(Order order) {
        this.order = order;

        lblTitle.setText(
                "Order " + order.getId() +
                        (order.getTavolo() > 0 ? " (Tav. " + order.getTavolo() + ")" : "")
        );

        lblSubtitle.setText(
                String.format("hours: %s; total: %.2f€",
                        order.getDataOra().format(TIME),
                        order.getTotale())
        );
    }

    @FXML
    private void onHover() {
        root.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #F9F9F9;");
    }

    @FXML
    private void onExit() {
        root.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white;");
    }

    @FXML
    private void onClick() {
        logger.log(Level.INFO, "Apro dettagli ordine #" + order.getId());
    }
}
