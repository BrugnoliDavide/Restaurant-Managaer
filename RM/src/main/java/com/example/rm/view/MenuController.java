package com.example.rm.view;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import com.example.rm.view.component.AddProductDialog;
import com.example.rm.view.component.ProductRowController;
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
import javafx.scene.control.TextField;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class MenuController {

    @FXML private VBox menuContainer;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch;

    private static final Logger logger =
            Logger.getLogger(MenuController.class.getName());

    private List<MenuProduct> allProductsMaster = new ArrayList<>();


    @FXML
    public void initialize() {
        setupManageHover();
        loadDataFromDB();
        setupSearchListener();
    }


    @FXML
    private void goBack() {
        View managerView = ViewFactory.forRole("manager");
        lblManage.getScene().setRoot(managerView.getRoot());
    }


    @FXML
    public void onAddProduct() {
        AddProductDialog.display();
        loadDataFromDB();
    }



    private void renderMenu(List<MenuProduct> products) {
        menuContainer.getChildren().clear();

        Map<String, List<MenuProduct>> datiMenu =
                products.stream().collect(Collectors.groupingBy(MenuProduct::getTipologia));

        datiMenu.keySet().stream().sorted().forEach(categoria -> {
            Label sectionTitle = new Label(categoria);
            sectionTitle.getStyleClass().add("menu-section-title");
            menuContainer.getChildren().add(sectionTitle);

            for (MenuProduct prodotto : datiMenu.get(categoria)) {
                menuContainer.getChildren().add(loadProductRow(prodotto));
            }
        });

        if (products.isEmpty()) {
            Label empty = new Label("Nessun prodotto trovato.");
            empty.setStyle("-fx-padding: 20; -fx-text-fill: #888; -fx-font-style: italic;");
            menuContainer.getChildren().add(empty);
        }
    }

    private void openProductDetail(MenuProduct product) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ProductDetail.fxml")
            );

            Parent root = loader.load();

            ProductDetailController controller =
                    loader.getController();

            controller.setProduct(product);

            menuContainer.getScene().setRoot(root);

        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Errore apertura ProductDetail", e);
        }
    }

    private void loadDataFromDB() {
        allProductsMaster = DatabaseService.getAllProducts();
        renderMenu(allProductsMaster);
    }

    private void setupSearchListener() {
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non iniettato correttamente da FXML");
            return;
        }

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filterAndRender(newVal);
        });
    }

    private Parent loadProductRow(MenuProduct prodotto) {
        try {
            FXMLLoader loader = new FXMLLoader(

                    getClass().getResource("/ProductRow.fxml")
            );
            Parent root = loader.load();

            ProductRowController controller = loader.getController();
            controller.setProduct(
                    prodotto,
                    this::reload,
                    this::openProductDetail
            );

            return root;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento ProductRow.fxml", e);
            return new Label("Errore caricamento prodotto");
        }
    }

    private void reload() {
        loadDataFromDB();
    }

    private void setupManageHover() {
        if (lblManage == null) return;

        String normal = "-fx-text-fill: #888; -fx-cursor: hand;";
        String hover  = "-fx-text-fill: #333; -fx-underline: true; -fx-cursor: hand;";

        lblManage.setStyle(normal);
        lblManage.setOnMouseEntered(e -> lblManage.setStyle(hover));
        lblManage.setOnMouseExited(e -> lblManage.setStyle(normal));
    }

    private void filterAndRender(String query) {
        String q = query == null ? "" : query.toLowerCase().trim();

        if (q.isEmpty()) {
            renderMenu(allProductsMaster);
            return;
        }

        List<MenuProduct> filtered = allProductsMaster.stream()
                .filter(p ->
                        (p.getNome() != null && p.getNome().toLowerCase().contains(q)) ||
                                (p.getTipologia() != null && p.getTipologia().toLowerCase().contains(q))
                )
                .collect(Collectors.toList());

        renderMenu(filtered);
    }


}
