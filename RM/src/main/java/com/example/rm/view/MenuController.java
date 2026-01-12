package com.example.rm.view;

import com.example.rm.controller.MenuService;
import com.example.rm.controller.MenuUseCase;
import com.example.rm.model.MenuProduct;
import com.example.rm.view.component.AddProductDialog;
import com.example.rm.view.component.ProductRowController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller per la vista del menu.
 * Gestisce la visualizzazione, ricerca e navigazione dei prodotti.
 */
public class MenuController {

    private static final Logger logger = Logger.getLogger(MenuController.class.getName());


    private static final String EMPTY_MESSAGE = "Nessun prodotto trovato.";
    private static final String ERROR_LOAD_MESSAGE = "Errore caricamento prodotto";



    private static MenuUseCase menuUseCase = new MenuService();

    public static void setMenuUseCase(MenuUseCase useCase) {
        menuUseCase = useCase;
    }

    @FXML private VBox menuContainer;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch;

    private List<MenuProduct> allProductsMaster = new ArrayList<>();

    @FXML
    public void initialize() {
        validateFXMLInjections();
        setupEventHandlers();
        loadDataFromDB();
    }

    private void validateFXMLInjections() {
        if (menuContainer == null) {
            logger.log(Level.SEVERE, "menuContainer non iniettato da FXML");
        }
        if (lblManage == null) {
            logger.log(Level.WARNING, "lblManage non iniettato da FXML");
        }
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non iniettato da FXML");
        }
    }

    private void setupEventHandlers() {
        setupSearchListener();
    }

    @FXML
    private void goBack() {
        try {
            View managerView = ViewFactory.forRole("manager");
            if (lblManage != null && lblManage.getScene() != null) {
                lblManage.getScene().setRoot(managerView.getRoot());
            } else {
                logger.log(Level.WARNING, "Impossibile navigare: scene non disponibile");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno al manager", e);
        }
    }

    @FXML
    public void onAddProduct() {
        try {
            AddProductDialog.display();
            loadDataFromDB();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'aggiunta del prodotto", e);
        }
    }

    private void setupSearchListener() {
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non disponibile per il listener");
            return;
        }
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                filterAndRender(newVal);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore durante il filtraggio", e);
            }
        });
    }

    private void loadDataFromDB() {
        try {
            allProductsMaster = menuUseCase.loadAllProducts();
            renderMenu(allProductsMaster);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti dal database", e);
            allProductsMaster = new ArrayList<>();
            renderMenu(allProductsMaster);
        }
    }

    private void reload() {
        loadDataFromDB();
    }

    private void renderMenu(List<MenuProduct> products) {
        if (menuContainer == null) {
            logger.log(Level.SEVERE, "Impossibile renderizzare: menuContainer è null");
            return;
        }

        menuContainer.getChildren().clear();

        if (products.isEmpty()) {
            renderEmptyState();
            return;
        }

        Map<String, List<MenuProduct>> productsByCategory = groupProductsByCategory(products);
        renderCategoriesWithProducts(productsByCategory);
    }

    private Map<String, List<MenuProduct>> groupProductsByCategory(List<MenuProduct> products) {
        return products.stream()
                .collect(Collectors.groupingBy(MenuProduct::getTipologia));
    }

    private void renderCategoriesWithProducts(Map<String, List<MenuProduct>> productsByCategory) {
        productsByCategory.keySet().stream()
                .sorted()
                .forEach(categoria -> {
                    renderCategoryHeader(categoria);
                    renderProductsForCategory(productsByCategory.get(categoria));
                });
    }

    private void renderCategoryHeader(String categoryName) {
        Label sectionTitle = new Label(categoryName);
        sectionTitle.getStyleClass().add("menu-section-title");
        menuContainer.getChildren().add(sectionTitle);
    }

    private void renderProductsForCategory(List<MenuProduct> products) {
        products.forEach(product -> {
            Parent productRow = loadProductRow(product);
            if (productRow != null) {
                menuContainer.getChildren().add(productRow);
            }
        });
    }

    private void renderEmptyState() {
        Label emptyLabel = new Label(EMPTY_MESSAGE);
        emptyLabel.getStyleClass().add("empty-state-label");
        menuContainer.getChildren().add(emptyLabel);
    }

    private Parent loadProductRow(MenuProduct product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ProductRow.fxml")
            );
            Parent root = loader.load();
            ProductRowController controller = loader.getController();
            controller.setProduct(product, this::reload, this::openProductDetail);
            return root;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento ProductRow.fxml per prodotto: " +
                    (product != null ? product.getNome() : "null"), e);
            return createErrorLabel();
        }
    }

    private Label createErrorLabel() {
        Label errorLabel = new Label(ERROR_LOAD_MESSAGE);
        errorLabel.getStyleClass().add("error-label");
        return errorLabel;
    }

    private void openProductDetail(MenuProduct product) {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di aprire dettaglio con prodotto null");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ProductDetail.fxml")
            );
            Parent root = loader.load();
            ProductDetailController controller = loader.getController();
            controller.setProduct(product);
            if (menuContainer != null && menuContainer.getScene() != null) {
                menuContainer.getScene().setRoot(root);
            } else {
                logger.log(Level.WARNING, "Scene non disponibile per la navigazione");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore apertura ProductDetail per prodotto: " +
                    product.getNome(), e);
        }
    }

    private void filterAndRender(String query) {
        String normalizedQuery = normalizeSearchQuery(query);
        if (normalizedQuery.isEmpty()) {
            renderMenu(allProductsMaster);
            return;
        }
        List<MenuProduct> filtered = filterProducts(normalizedQuery);
        renderMenu(filtered);
    }

    private String normalizeSearchQuery(String query) {
        return query == null ? "" : query.toLowerCase().trim();
    }

    private List<MenuProduct> filterProducts(String query) {
        return allProductsMaster.stream()
                .filter(product -> matchesSearchQuery(product, query))
                .collect(Collectors.toList());
    }

    private boolean matchesSearchQuery(MenuProduct product, String query) {
        return matchesName(product, query) || matchesCategory(product, query);
    }

    private boolean matchesName(MenuProduct product, String query) {
        return product.getNome() != null &&
                product.getNome().toLowerCase().contains(query);
    }

    private boolean matchesCategory(MenuProduct product, String query) {
        return product.getTipologia() != null &&
                product.getTipologia().toLowerCase().contains(query);
    }
}
