package com.example.rm.view.component;

import com.example.rm.model.MenuProduct;
import javafx.fxml.FXML;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import java.util.function.Consumer;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.dao.ProductDAO;



public class ProductRowController {

    @FXML private HBox root;
    @FXML private Label lblName;
    @FXML private Label lblDetails;
    @FXML private Label dots;

    private final ProductDAO productDAO = new DatabaseProductDAO();

    private Consumer<MenuProduct> onSelect;

    private MenuProduct product;
    private Runnable reloadCallback;
    private ContextMenu contextMenu;

    public void setProduct(MenuProduct product, Runnable reloadCallback) {
        this.product = product;
        this.reloadCallback = reloadCallback;

        lblName.setText(product.getNome());

        long quantitySold =
                productDAO.getQuantitySold(product.getNome());

        lblDetails.setText(
                String.format("%.2f€ | Q.ta: %d",
                        product.getPrezzoVendita(),
                        quantitySold)
        );

        setupContextMenu();
    }


    @FXML
    private void onHover() {
        root.setStyle("-fx-background-color: #F9F9F9;");
    }

    @FXML
    private void onExit() {
        root.setStyle("-fx-background-color: white;");
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


    private void setupContextMenu() {

        contextMenu = new ContextMenu();

        MenuItem delete = new MenuItem("Elimina prodotto");
        delete.setOnAction(e -> {
            boolean success = productDAO.delete((long) product.getId());

            if (success && reloadCallback != null) {
                reloadCallback.run();
            }
        });

        contextMenu.getItems().add(delete);
    }

    public void setProduct(
            MenuProduct product,
            Runnable reloadCallback,
            Consumer<MenuProduct> onSelect
    ) {
        this.product = product;
        this.reloadCallback = reloadCallback;
        this.onSelect = onSelect;

        lblName.setText(product.getNome());

        long quantitySold =
                productDAO.getQuantitySold(product.getNome());

        lblDetails.setText(
                String.format("%.2f€ | Q.ta: %d",
                        product.getPrezzoVendita(),
                        quantitySold)
        );
        setupContextMenu();
    }


    @FXML
    private void onClick(MouseEvent event) {

        if (event.getTarget() == dots) {
            return;
        }

        if (onSelect != null) {
            onSelect.accept(product);
        }
    }
}
