package com.example.rm.view;

import com.example.rm.app.SceneManager;
import com.example.rm.controller.FinancialService;
import com.example.rm.controller.FinancialUseCase;
import com.example.rm.model.Order;
import com.example.rm.service.LoggerService;
import com.example.rm.view.screens.OrderDetailView;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller per la vista Financial.
 * Gestisce la visualizzazione degli ordini con filtri e navigazione ai dettagli.
 */
public class FinancialController {

    @FXML private ListView<Order> ordersListView;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch;

    private static final Logger logger = LoggerService.getLogger(FinancialController.class);

    private static FinancialUseCase financialUseCase = new FinancialService();

    public static void setFinancialUseCase(FinancialUseCase useCase) {
        financialUseCase = useCase;
    }

    private List<Order> allOrdersMaster = new ArrayList<>();

    @FXML
    public void initialize() {
        setupSearchListener();
        ordersListView.setCellFactory(listView -> new OrderCell());
        loadDataFromDB();
    }

    private void loadDataFromDB() {
        try {
            allOrdersMaster = financialUseCase.loadAllOrdersWithDisplayItems();
            ordersListView.getItems().setAll(allOrdersMaster);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore loadDataFromDB", e);
            allOrdersMaster = new ArrayList<>();
        }
    }

    private void setupSearchListener() {
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non disponibile per il listener");
            return;
        }

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                filterAndRender(newValue);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore durante il filtraggio degli ordini", e);
            }
        });
    }

    private void filterAndRender(String query) {
        String normalized = normalizeSearchQuery(query);
        if (normalized.isEmpty()) {
            ordersListView.getItems().setAll(allOrdersMaster);
        } else {
            ordersListView.getItems().setAll(filterOrders(normalized));
        }
    }

    private String normalizeSearchQuery(String query) {
        return query == null ? "" : query.toLowerCase().trim();
    }

    private List<Order> filterOrders(String query) {
        return allOrdersMaster.stream()
                .filter(order -> matchesSearchQuery(order, query))
                .toList();
    }

    private boolean matchesSearchQuery(Order order, String query) {
        return matchesOrderId(order, query)
                || matchesTable(order, query)
                || matchesTotal(order, query)
                || matchesDate(order, query);
    }

    private boolean matchesOrderId(Order order, String query) {
        return String.valueOf(order.getId()).contains(query);
    }

    private boolean matchesTable(Order order, String query) {
        return String.valueOf(order.getTavolo()).contains(query);
    }

    private boolean matchesTotal(Order order, String query) {
        return String.valueOf(order.getTotale()).contains(query);
    }

    private boolean matchesDate(Order order, String query) {
        return order.getDataOra() != null &&
                order.getDataOra().toString().toLowerCase().contains(query);
    }

    @FXML
    private void goBack() {
        try {
            SceneManager.showManager();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno al manager", e);
        }
    }

    private class OrderCell extends ListCell<Order> {
        private final HBox root = new HBox(10);
        private final Label lblId = new Label();
        private final Label lblTable = new Label();
        private final Label lblTotal = new Label();
        private final Label lblStatus = new Label();

        OrderCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(8, 12, 8, 12));
            root.getStyleClass().add("order-row");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            root.getChildren().addAll(lblId, lblTable, lblStatus, spacer, lblTotal);

            setOnMouseClicked(e -> {
                if (getItem() != null) navigateToOrderDetail(getItem());
            });
        }

        @Override
        protected void updateItem(Order order, boolean empty) {
            super.updateItem(order, empty);
            if (empty || order == null) {
                setGraphic(null);
            } else {
                lblId.setText("#" + order.getId());
                lblTable.setText("Tavolo " + order.getTavolo());
                lblTotal.setText(String.format("€%.2f", order.getTotale()));
                lblStatus.setText(order.getStatus());
                setGraphic(root);
            }
        }
        private void navigateToOrderDetail(Order order) {
            if (order == null) return;
            try {
                OrderDetailView detailView = new OrderDetailView(order);
                if (ordersListView.getScene() != null) {
                    ordersListView.getScene().setRoot(detailView.getRoot());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore navigazione dettaglio ordine", e);
            }
        }
    }
}