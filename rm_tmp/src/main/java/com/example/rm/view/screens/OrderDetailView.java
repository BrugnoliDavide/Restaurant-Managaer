package com.example.rm.view.screens;

import com.example.rm.model.Order;
import com.example.rm.view.OrderDetailController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Vista dedicata per i dettagli di un ordine
 * Utilizzata in FinancialView per mostrare informazioni complete
 */
public class OrderDetailView implements com.example.rm.view.View {

    private final Parent root;
    private final Order order;

    /**
     * Costruttore che carica l'FXML e passa l'ordine al controller
     * @param order L'ordine da visualizzare
     */
    public OrderDetailView(Order order) {
        this.order = order;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/OrderDetailView.fxml")
            );

            // Carica l'FXML
            this.root = loader.load();

            // Ottieni il controller e passa l'ordine
            OrderDetailController controller = loader.getController();
            if (controller != null) {
                controller.setOrder(order);
            } else {
                throw new IllegalStateException(
                        "Controller non trovato in OrderDetailView.fxml"
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossibile caricare OrderDetailView.fxml", e
            );
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    public Order getOrder() {
        return order;
    }
}
