package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.view.component.OrderReviewDialog;
import com.example.rm.view.component.ProductRowFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.example.rm.controller.MenuUseCase;
import com.example.rm.controller.OrderUseCase;
import com.example.rm.controller.MenuService;
import com.example.rm.controller.OrderService;
import com.example.rm.app.SceneManager;



  //Gestisce il carrello e l'invio degli ordini al database.
public class TakeOrderController {

    private static final Logger logger = Logger.getLogger(TakeOrderController.class.getName());

    @FXML private Label lblTitle;
    @FXML private VBox productsContainer;
    @FXML private Button btnSend;

    private int numeroTavolo;

    private final Map<Integer, OrderItem> carrello = new HashMap<>();
    private static final MenuUseCase menuUseCase = new MenuService();
    private static final OrderUseCase orderUseCase = new OrderService();
    private static final User currentUser = UserSession.getInstance().getUser();

    private static final String erroreString = "ERRORE: ";
    
    public void init(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
        updateTitle();
        loadProducts();
        updateSendButton();
    }

    private void updateTitle() {
        if (lblTitle != null) {
            lblTitle.setText("Select item (Tav. " + numeroTavolo + ")");
        }
    }

    private void loadProducts() {
        if (productsContainer == null) {
            logger.log(Level.SEVERE, "productsContainer non inizializzato");
            showErrorAlert(erroreString, "Sessione utente scaduta. Rilogga.");
            return;
        }

        try {
            List<MenuProduct> prodotti = menuUseCase.loadAllProducts();

            renderProducts(prodotti);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento prodotti", e);
        }
    }

    private void renderProducts(List<MenuProduct> prodotti) {
        productsContainer.getChildren().clear();

        for (MenuProduct prodotto : prodotti) {
            HBox productRow = createProductRow(prodotto);
            productsContainer.getChildren().add(productRow);
        }
    }

    private HBox createProductRow(MenuProduct prodotto) {
        int currentQuantity = getCurrentQuantity(prodotto.getId());
        String formattedPrice = formatPrice(prodotto.getPrezzoVendita());

        return ProductRowFactory.row(
                prodotto.getNome(),
                prodotto.getTipologia(),
                formattedPrice,
                currentQuantity,
                () -> handleIncrementProduct(prodotto),
                () -> handleDecrementProduct(prodotto)
        );
    }

    private int getCurrentQuantity(int productId) {
        return carrello.containsKey(productId)
                ? carrello.get(productId).getQuantita()
                : 0;
    }

    private String formatPrice(double price) {
        return "€ " + String.format("%.2f", price);
    }

    private void handleIncrementProduct(MenuProduct prodotto) {
        incrementProductQuantity(prodotto);
        loadProducts();
    }

    private void handleDecrementProduct(MenuProduct prodotto) {
        decrementProductQuantity(prodotto);
        loadProducts();
    }

    private void incrementProductQuantity(MenuProduct prodotto) {
        if (carrello.containsKey(prodotto.getId())) {
            // Prodotto già presente, incrementa quantità
            OrderItem existingItem = carrello.get(prodotto.getId());
            OrderItem updatedItem = createOrderItem(
                    prodotto,
                    existingItem.getQuantita() + 1
            );
            carrello.put(prodotto.getId(), updatedItem);
        } else {
            // Nuovo prodotto
            OrderItem newItem = createOrderItem(prodotto, 1);
            carrello.put(prodotto.getId(), newItem);
        }

        updateSendButton();

        logger.log(Level.INFO, "Prodotto aggiunto: {0} (Quantità: {1})",
                new Object[]{prodotto.getNome(), carrello.get(prodotto.getId()).getQuantita()});
    }

    private void decrementProductQuantity(MenuProduct prodotto) {
        if (!carrello.containsKey(prodotto.getId())) {
            return;
        }

        OrderItem existingItem = carrello.get(prodotto.getId());
        int currentQuantity = existingItem.getQuantita();

        if (currentQuantity > 1) {
            // Decrementa quantità
            OrderItem updatedItem = createOrderItem(prodotto, currentQuantity - 1);
            carrello.put(prodotto.getId(), updatedItem);
        } else {
            // Rimuovi dal carrello
            carrello.remove(prodotto.getId());
        }

        updateSendButton();

        logger.log(Level.INFO, "Prodotto rimosso: {0}", prodotto.getNome());
    }

    private OrderItem createOrderItem(MenuProduct prodotto, int quantita) {
        return new OrderItem(
                prodotto,
                quantita,
                prodotto.getPrezzoVendita(),      // prezzoSnapshot
                prodotto.getCostoRealizzazione(), // costoSnapshot
                prodotto.getNome()                // nomeSnapshot
        );
    }

    private void clearCart() {
        carrello.clear();
        updateSendButton();
    }

    private void updateSendButton() {
        if (btnSend == null) {
            return;
        }

        int totalItems = calculateTotalItems();
        btnSend.setText("Send Order (" + totalItems + ")");
        btnSend.setDisable(totalItems == 0);
    }

    private int calculateTotalItems() {
        return carrello.values().stream()
                .mapToInt(OrderItem::getQuantita)
                .sum();
    }
    @FXML
    private void handleCancel() {
        try {
            SceneManager.showWaiter();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'annullamento", e);
        }
    }

    @FXML
    private void handleSend() {
        if (carrello.isEmpty()) {
            showInfoAlert("Carrello vuoto", "Seleziona almeno un prodotto.");
            return;
        }
        btnSend.setDisable(true);
        showOrderReviewDialog();
    }

    private void showOrderReviewDialog() {
        try {
            Stage stage = getStage();
            if (stage == null) {
                logger.log(Level.WARNING, "Stage non disponibile per il dialog");
                return;
            }

            List<OrderItem> items = new ArrayList<>(carrello.values());

            OrderReviewDialog.show(stage, numeroTavolo, items, "", result -> {
                if (result.confirmed) {
                    sendOrderToDatabase(items, result.note);
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore apertura dialog review", e);
            showErrorAlert(erroreString, "Impossibile aprire il dialog di conferma");
        }
    }

    private void sendOrderToDatabase(List<OrderItem> items, String note) {
        try {
            boolean success = orderUseCase.createOrder(
                    items,
                    numeroTavolo,
                    note,
                    currentUser
            );

            if (success) {
                handleOrderSuccess();
            } else {
                handleOrderFailure();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore invio ordine al database", e);
            showErrorAlert(erroreString, "Errore durante l'invio dell'ordine: " + e.getMessage());
        }
    }

    private void handleOrderSuccess() {
        logger.log(Level.INFO, "Ordine inviato con successo per tavolo {0}", numeroTavolo);
        showInfoAlert("Successo", "Ordine inviato con successo!");
        clearCart();
        SceneManager.showWaiter();
    }

    private void handleOrderFailure() {
        logger.log(Level.SEVERE, "Impossibile inviare ordine per tavolo {0}", numeroTavolo);
        showErrorAlert(erroreString, "Impossibile inviare l'ordine al database.");
    }

    private Stage getStage() {
        if (btnSend == null || btnSend.getScene() == null) {
            return null;
        }
        return (Stage) btnSend.getScene().getWindow();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}