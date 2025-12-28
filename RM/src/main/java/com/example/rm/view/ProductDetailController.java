package com.example.rm.view;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.LoggerService;
import com.example.rm.view.component.AddProductDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDetailController {

    @FXML private Label lblBack;
    @FXML private Label lblName;
    @FXML private VBox contentBox;

    private static final Logger logger =
            LoggerService.getLogger(ProductDetailController.class);

    private MenuProduct product;

    public void setProduct(MenuProduct product) {
        this.product = product;
        render();
    }

    private void render() {
        if (product == null) return;

        lblName.setText(product.getNome());
    }

    @FXML
    private void initialize() {
        setupBackHover();
    }

    @FXML
    private void goBack() {
        logger.log(Level.INFO, "invocazione GoBack");
        View menuView = ViewFactory.forRole("menu");
        lblBack.getScene().setRoot(menuView.getRoot());
    }

    @FXML
    private void onDelete() {
        boolean success = DatabaseService.deleteProduct(product.getId());
        if (success) {
            goBack();
        } else {
            logger.log(Level.SEVERE, "Impossibile eliminare il prodotto");
            // TODO: Mostrare un Alert all'utente per notificare l'errore
        }
    }

    @FXML
    private void onEdit() {
        // AddProductDialog.displayEdit() è void, quindi non possiamo
        // verificare il successo. da gestire internamente
        AddProductDialog.displayEdit(product);

        // Torna sempre al menu dopo la chiamata al dialog (anche in caso di errore !! )
        goBack();
    }

    private void setupBackHover() {
        String normal = "-fx-text-fill: #888; -fx-cursor: hand;";
        String hover  = "-fx-text-fill: #333; -fx-underline: true; -fx-cursor: hand;";

        lblBack.setStyle(normal);
        lblBack.setOnMouseEntered(e -> lblBack.setStyle(hover));
        lblBack.setOnMouseExited(e -> lblBack.setStyle(normal));
    }
}