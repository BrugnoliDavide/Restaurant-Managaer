package com.example.demo.view;

import com.example.demo.model.MenuProduct;
import com.example.demo.service.DatabaseService;
import com.example.demo.view.component.AddProductDialog;
import com.example.demo.view.component.ProductRowFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ProductDetailController {

    @FXML private Label lblBack;
    @FXML private Label lblName;
    @FXML private VBox contentBox;

    private MenuProduct product;

    public void setProduct(MenuProduct product) {
        this.product = product;
        lblName.setText(product.getNome());
        loadData();
    }

    @FXML
    private void initialize() {
        setupBackHover();
    }

    @FXML
    private void goBack() {
        View menuView = ViewFactory.forRole("menu");
        lblBack.getScene().setRoot(menuView.getRoot());
    }

    @FXML
    private void onDelete() {
        boolean success = DatabaseService.deleteProduct(product.getId());
        if (success) {
            goBack();
        }
    }

    @FXML
    private void onEdit() {
        AddProductDialog.displayEdit(product);
        goBack();
    }

    private void loadData() {
        long quantitySold = DatabaseService.getQuantitySold(product.getNome());

        double income = (product.getPrezzoVendita() - product.getCostoRealizzazione()) * quantitySold;

        // Correzione: Aggiunto 'null' come quarto argomento (onAction) per ogni chiamata a ProductRowFactory.row
        contentBox.getChildren().setAll(
                ProductRowFactory.row(
                        "Price",
                        "",
                        String.format("%.2f€", product.getPrezzoVendita()),
                        0,    // Quantità non rilevante qui
                        null, // Nessuna azione add
                        null
                ),

                ProductRowFactory.row(
                        "Sold Quantity",
                        "last 30 days",
                        String.valueOf(quantitySold),
                        0,    // Quantità non rilevante qui
                        null, // Nessuna azione add
                        null
                ),

                ProductRowFactory.row(
                        "Realization Price",
                        "",
                        String.format("%.2f€", product.getCostoRealizzazione()),
                        0,    // Quantità non rilevante qui
                        null, // Nessuna azione add
                        null
                ),

                ProductRowFactory.row(
                        "Realized Income",
                        "Total profit",
                        String.format("%.2f€", income),
                        0,    // Quantità non rilevante qui
                        null, // Nessuna azione add
                        null
                )
        );
    }

    private void setupBackHover() {
        String normal = "-fx-text-fill: #888; -fx-cursor: hand;";
        String hover = "-fx-text-fill: #333; -fx-underline: true; -fx-cursor: hand;";

        lblBack.setStyle(normal);
        lblBack.setOnMouseEntered(e -> lblBack.setStyle(hover));
        lblBack.setOnMouseExited(e -> lblBack.setStyle(normal));
    }
}