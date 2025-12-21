package com.example.demo.view;

import com.example.demo.model.MenuProduct;
import com.example.demo.model.OrderItem;
import com.example.demo.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TakeOrderController {

    @FXML private Label lblTitle;
    @FXML private VBox productsContainer;

    private int numeroTavolo;

    private final Map<Integer, OrderItem> carrello = new HashMap<>();

    /* inizializzazione con parametro */
    public void init(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
        lblTitle.setText("Select item (Tav. " + numeroTavolo + ")");
        loadProducts();
    }



    private void loadProducts() {
        List<MenuProduct> prodotti = DatabaseService.getAllProducts();
        productsContainer.getChildren().clear();

        for (MenuProduct p : prodotti) {
            Label lbl = new Label(p.getNome() + " - €" + p.getPrezzoVendita());
            productsContainer.getChildren().add(lbl);
        }
    }

    /*
    private void loadProducts() {
        List<MenuProduct> prodotti = DatabaseService.getAllProducts();
        productsContainer.getChildren().clear();

        for (MenuProduct p : prodotti) {
            productsContainer.getChildren().add(
                    ProductRowFactory.create(p, carrello)
            );
        }
    }*/

    @FXML
    private void handleCancel() {
        View waiterView = ViewFactory.forRole("waiter");
        productsContainer.getScene().setRoot(waiterView.getRoot());
    }

    @FXML
    private void handleSend() {
        if (carrello.isEmpty()) {
            showAlert("Carrello vuoto", "Seleziona almeno un prodotto.");
            return;
        }

        boolean success = DatabaseService.createOrder(
                carrello.values().stream().toList(),
                numeroTavolo,
                ""
        );

        View waiterView = ViewFactory.forRole("waiter");
        productsContainer.getScene().setRoot(waiterView.getRoot());
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
