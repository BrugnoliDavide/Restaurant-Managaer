package com.example.rm.view.component;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class OrderRowController {

    // IMPORTANTE: Questi fx:id DEVONO corrispondere a quelli in OrderRow.fxml
    @FXML private HBox root;
    @FXML private Label lblTitle;      // Corrisponde a fx:id="lblTitle"
    @FXML private Label lblSubtitle;   // Corrisponde a fx:id="lblSubtitle"

    private Order order;
    private Consumer<Order> onClickCallback;

    private static final Logger logger =
            Logger.getLogger(OrderRowController.class.getName());

    /**
     * Imposta l'ordine da visualizzare
     * @param order L'ordine da visualizzare
     * @param onClickCallback Callback per il click (può essere null)
     */
    public void setOrder(Order order, Consumer<Order> onClickCallback) {
        if (order == null) {
            logger.warning("Tentativo di impostare un ordine NULL");
            return;
        }

        this.order = order;
        this.onClickCallback = onClickCallback;

        updateUI();
    }

    /**
     * Aggiorna l'interfaccia con i dati dell'ordine
     */
    private void updateUI() {
        // TITLE: "Ordine #123 - Tavolo 5 - €45.50"
        if (lblTitle != null) {
            String title = String.format("Ordine #%d - Tavolo %d - €%.2f",
                    order.getId(),
                    order.getTavolo(),
                    order.getTotale());
            lblTitle.setText(title);
        } else {
            logger.warning("lblTitle è null - verificare fx:id in OrderRow.fxml");
        }

        // SUBTITLE: "18:30 - 2x Pizza, 1x Pasta"
        if (lblSubtitle != null) {
            StringBuilder subtitle = new StringBuilder();

            // Aggiungi orario
            if (order.getDataOra() != null) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                subtitle.append(order.getDataOra().format(timeFormatter));
            }

            // Aggiungi dettagli ordine
            List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());
            if (!items.isEmpty()) {
                subtitle.append(" - ");
                subtitle.append(String.join(", ", items));
            }

            lblSubtitle.setText(subtitle.toString());
        } else {
            logger.warning("lblSubtitle è null - verificare fx:id in OrderRow.fxml");
        }
    }

    // ========================================
    // GESTORI EVENTI (chiamati dall'FXML)
    // ========================================

    @FXML
    private void onHover() {
        if (root != null) {
            root.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; " +
                    "-fx-background-color: #F9F9F9; -fx-cursor: hand;");
        }
    }

    @FXML
    private void onExit() {
        if (root != null) {
            root.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; " +
                    "-fx-background-color: white; -fx-cursor: hand;");
        }
    }

    @FXML
    private void onClick() {
        logger.info("Click su ordine #" + order.getId());

        if (onClickCallback != null) {
            onClickCallback.accept(order);
        }
    }
}