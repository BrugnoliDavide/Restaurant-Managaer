package com.example.demo.view.component;

import com.example.demo.model.MenuProduct;
import com.example.demo.service.DatabaseService;
import com.example.demo.view.ProductDetailController;
import com.example.demo.view.View;
import com.example.demo.view.ViewFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class ProductRowController {

    /* =========================
       FXML BINDINGS
       ========================= */

    @FXML private HBox root;
    @FXML private Label lblName;
    @FXML private Label lblDetails;
    @FXML private Label dots;

    /* =========================
       STATE
       ========================= */

    private MenuProduct product;
    private Runnable reloadCallback;
    private ContextMenu contextMenu;

    /* =========================
       SETUP
       ========================= */

    public void setProduct(MenuProduct product, Runnable reloadCallback) {
        this.product = product;
        this.reloadCallback = reloadCallback;

        lblName.setText(product.getNome());

        long quantitySold =
                DatabaseService.getQuantitySold(product.getNome());

        lblDetails.setText(
                String.format("%.2f€ | Q.ta: %d",
                        product.getPrezzoVendita(),
                        quantitySold)
        );

        setupContextMenu();
    }

    /* =========================
       EVENTS
       ========================= */

    @FXML
    private void onHover() {
        root.setStyle("-fx-background-color: #F9F9F9;");
    }

    @FXML
    private void onExit() {
        root.setStyle("-fx-background-color: white;");
    }

    @FXML
    private void onClick(MouseEvent event) {

        if (event.getTarget() == dots) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ProductDetail.fxml")
            );

            Parent rootView = loader.load();

            ProductDetailController controller =
                    loader.getController();

            controller.setProduct(product);

            root.getScene().setRoot(rootView);

        } catch (IOException e) {
            //da cambiare
            e.printStackTrace();
        }
    }


    @FXML
    private void onDotsClick(MouseEvent event) {
        event.consume();
        contextMenu.show(
                dots,
                javafx.geometry.Side.BOTTOM,
                0, 0
        );
    }

    /* =========================
       CONTEXT MENU
       ========================= */

    private void setupContextMenu() {

        contextMenu = new ContextMenu();

        MenuItem delete = new MenuItem("Elimina prodotto");
        delete.setOnAction(e -> {
            boolean success =
                    DatabaseService.deleteProduct(product.getId());

            if (success && reloadCallback != null) {
                reloadCallback.run();
            }
        });

        contextMenu.getItems().add(delete);
    }
}
