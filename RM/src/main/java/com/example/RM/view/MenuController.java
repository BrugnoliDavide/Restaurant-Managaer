package com.example.RM.view;

import com.example.RM.model.MenuProduct;
import com.example.RM.service.DatabaseService;
import com.example.RM.view.component.AddProductDialog;
import com.example.RM.view.component.ProductRowController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import java.util.logging.Logger;

public class MenuController {

    @FXML private VBox menuContainer;
    @FXML private Label lblManage;

    private static final Logger logger =
            Logger.getLogger(MenuController.class.getName());

    @FXML
    public void initialize() {
        setupManageHover();
        reload();
    }

    // ---------------- NAVIGAZIONE ----------------

    @FXML
    private void goBack() {
        View managerView = ViewFactory.forRole("manager");
        lblManage.getScene().setRoot(managerView.getRoot());
    }


    @FXML
    public void onAddProduct() {
        AddProductDialog.display();
        reload();
    }

    // ---------------- LOGICA MENU ----------------

    private void loadMenu() {

        menuContainer.getChildren().clear();

        Map<String, List<MenuProduct>> datiMenu =
                DatabaseService.getMenuByCategories();

        for (String categoria : datiMenu.keySet()) {

            Label sectionTitle = new Label(categoria);
            sectionTitle.getStyleClass().add("menu-section-title");


            menuContainer.getChildren().add(sectionTitle);

            for (MenuProduct prodotto : datiMenu.get(categoria)) {
                menuContainer.getChildren().add(loadProductRow(prodotto));
            }
        }
    }

    private Parent loadProductRow(MenuProduct prodotto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ProductRow.fxml")
            );
            Parent root = loader.load();

            ProductRowController controller = loader.getController();
            controller.setProduct(prodotto, this::reload);

            return root;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento ProductRow.fxml", e);
            return new Label("Errore caricamento prodotto");
        }
    }

    private void reload() {
        loadMenu();
    }

    // ---------------- UI ----------------

    private void setupManageHover() {
        if (lblManage == null) return;

        String normal = "-fx-text-fill: #888; -fx-cursor: hand;";
        String hover  = "-fx-text-fill: #333; -fx-underline: true; -fx-cursor: hand;";

        lblManage.setStyle(normal);
        lblManage.setOnMouseEntered(e -> lblManage.setStyle(hover));
        lblManage.setOnMouseExited(e -> lblManage.setStyle(normal));
    }
}
