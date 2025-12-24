package com.example.demo.view;

import com.example.demo.model.MenuProduct;
import com.example.demo.model.OrderItem;
import com.example.demo.service.DatabaseService;
import com.example.demo.view.component.ProductRowFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TakeOrderController {

    private static final Logger logger = Logger.getLogger(TakeOrderController.class.getName());

    @FXML private Label lblTitle;
    @FXML private VBox productsContainer;
    @FXML private Button btnSend;

    private int numeroTavolo;
    // La mappa usa l'ID del prodotto come chiave per gestire le quantità
    private final Map<Integer, OrderItem> carrello = new HashMap<>();

    public void init(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
        lblTitle.setText("Select item (Tav. " + numeroTavolo + ")");
        loadProducts();
        updateSendButton();
    }

    private void loadProducts() {
        List<MenuProduct> prodotti = DatabaseService.getAllProducts(); //
        productsContainer.getChildren().clear();

        for (MenuProduct p : prodotti) {
            // Recuperiamo la quantità attuale dal carrello
            int currentQty = carrello.containsKey(p.getId()) ? carrello.get(p.getId()).getQuantita() : 0;

            HBox productRow = ProductRowFactory.row(
                    p.getNome(),
                    p.getTipologia(),
                    "€ " + String.format("%.2f", p.getPrezzoVendita()),
                    currentQty,
                    () -> { // Azione PIÙ
                        aggiungiProdotto(p);
                        loadProducts(); // Rinfresca la UI
                    },
                    () -> { // Azione MENO
                        rimuoviProdotto(p);
                        loadProducts(); // Rinfresca la UI
                    }
            );

            productsContainer.getChildren().add(productRow);
        }
    }

    private void aggiungiProdotto(MenuProduct p) {
        if (carrello.containsKey(p.getId())) {
            int qty = carrello.get(p.getId()).getQuantita();
            carrello.put(p.getId(), new OrderItem(p, qty + 1)); //
        } else {
            carrello.put(p.getId(), new OrderItem(p, 1)); //
        }
        updateSendButton();
    }

    private void rimuoviProdotto(MenuProduct p) {
        if (carrello.containsKey(p.getId())) {
            int qty = carrello.get(p.getId()).getQuantita();
            if (qty > 1) {
                carrello.put(p.getId(), new OrderItem(p, qty - 1));
            } else {
                carrello.remove(p.getId()); // Arrivato a zero, elimina dal carrello
            }
        }
        updateSendButton();
    }

    private void aggiungiAlCarrello(MenuProduct p) {
        if (carrello.containsKey(p.getId())) {
            // Poiché OrderItem non ha un setter per la quantità nel tuo file,
            // ne creiamo uno nuovo con la quantità aggiornata
            int vecchiaQuantita = carrello.get(p.getId()).getQuantita();
            carrello.put(p.getId(), new OrderItem(p, vecchiaQuantita + 1));
        } else {
            // Costruttore corretto: OrderItem(MenuProduct product, int quantita)
            carrello.put(p.getId(), new OrderItem(p, 1));
        }
        updateSendButton();
        logger.info("Prodotto aggiunto: " + p.getNome() + " (Totale: " + carrello.get(p.getId()).getQuantita() + ")");
    }

    private void updateSendButton() {
        int totalePezzi = carrello.values().stream().mapToInt(OrderItem::getQuantita).sum();
        btnSend.setText("Send Order (" + totalePezzi + ")");
        btnSend.setDisable(totalePezzi == 0);
    }

    @FXML
    private void handleCancel() {
        // Correzione ruolo per ViewFactory
        View waiterView = ViewFactory.forRole("cameriere");
        btnSend.getScene().setRoot(waiterView.getRoot());
    }

    @FXML
    private void handleSend() {
        if (carrello.isEmpty()) {
            showAlert("Carrello vuoto", "Seleziona almeno un prodotto.");
            return;
        }

        // Conversione della mappa in lista per il database
        List<OrderItem> items = carrello.values().stream().toList();

        // DatabaseService.createOrder(List<OrderItem> items, Integer tavolo, String note)
        boolean success = DatabaseService.createOrder(items, numeroTavolo, "");

        if (success) {
            View waiterView = ViewFactory.forRole("cameriere");
            btnSend.getScene().setRoot(waiterView.getRoot());
        } else {
            showAlert("Errore", "Impossibile inviare l'ordine al database.");
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}