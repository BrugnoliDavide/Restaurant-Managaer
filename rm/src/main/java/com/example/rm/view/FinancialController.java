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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller per la vista Financial.
 * Gestisce la visualizzazione degli ordini con filtri testuali e temporali,
 * raggruppamento per giorno e navigazione ai dettagli.
 */
public class FinancialController {

    @FXML private ListView<Order> ordersListView;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private Label lblClearDates;

    private static final Logger logger = LoggerService.getLogger(FinancialController.class);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN);

    private static FinancialUseCase financialUseCase = new FinancialService();

    public static void setFinancialUseCase(FinancialUseCase useCase) {
        financialUseCase = useCase;
    }

    private List<Order> allOrdersMaster = new ArrayList<>();

    @FXML
    public void initialize() {
        setupSearchListener();
        setupDateListeners();
        ordersListView.setCellFactory(listView -> new OrderCell());
        loadDataFromDB();
    }

    private void loadDataFromDB() {
        try {
            allOrdersMaster = financialUseCase.loadAllOrdersWithDisplayItems();
            applyAllFilters();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore loadDataFromDB", e);
            allOrdersMaster = new ArrayList<>();
        }
    }

    // =========================================================================
    //  Listener di ricerca e filtri temporali
    // =========================================================================

    private void setupSearchListener() {
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non disponibile per il listener");
            return;
        }

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                applyAllFilters();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore durante il filtraggio degli ordini", e);
            }
        });
    }

    private void setupDateListeners() {
        if (dateFrom != null) {
            dateFrom.valueProperty().addListener((obs, oldVal, newVal) -> applyAllFilters());
        }
        if (dateTo != null) {
            dateTo.valueProperty().addListener((obs, oldVal, newVal) -> applyAllFilters());
        }
    }

    // =========================================================================
    //  Logica di filtraggio combinato (testo + range temporale)
    // =========================================================================

    private void applyAllFilters() {
        String query = normalizeSearchQuery(txtSearch != null ? txtSearch.getText() : "");
        LocalDate from = dateFrom != null ? dateFrom.getValue() : null;
        LocalDate to = dateTo != null ? dateTo.getValue() : null;

        List<Order> filtered = allOrdersMaster.stream()
                .filter(order -> matchesDateRange(order, from, to))
                .filter(order -> query.isEmpty() || matchesSearchQuery(order, query))
                .toList();

        ordersListView.getItems().setAll(filtered);
    }

    private boolean matchesDateRange(Order order, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        LocalDateTime dataOra = order.getDataOra();
        if (dataOra == null) {
            return false;
        }
        LocalDate orderDate = dataOra.toLocalDate();

        if (from != null && orderDate.isBefore(from)) {
            return false;
        }
        return to == null || !orderDate.isAfter(to);
    }

    @FXML
    private void clearDateFilters() {
        if (dateFrom != null) {
            dateFrom.setValue(null);
        }
        if (dateTo != null) {
            dateTo.setValue(null);
        }
    }

    // =========================================================================
    //  Filtri testuali (invariati rispetto alla versione precedente)
    // =========================================================================

    private String normalizeSearchQuery(String query) {
        return query == null ? "" : query.toLowerCase().trim();
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

    // =========================================================================
    //  Navigazione
    // =========================================================================

    @FXML
    private void goBack() {
        try {
            SceneManager.showManager();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno al manager", e);
        }
    }

    // =========================================================================
    //  Cella personalizzata con orario e separatore giornaliero
    // =========================================================================

    private class OrderCell extends ListCell<Order> {

        private final VBox cellContainer = new VBox();
        private final Label lblDaySeparator = new Label();
        private final HBox orderRow = new HBox(10);

        private final Label lblId = new Label();
        private final Label lblTime = new Label();
        private final Label lblTable = new Label();
        private final Label lblTotal = new Label();
        private final Label lblStatus = new Label();

        OrderCell() {
            // Separatore giornaliero
            lblDaySeparator.getStyleClass().add("day-separator");
            lblDaySeparator.setMaxWidth(Double.MAX_VALUE);
            lblDaySeparator.setPadding(new Insets(12, 12, 4, 12));

            // Riga ordine
            orderRow.setAlignment(Pos.CENTER_LEFT);
            orderRow.setPadding(new Insets(8, 12, 8, 12));
            orderRow.getStyleClass().add("order-row");

            lblTime.getStyleClass().add("order-time");
            lblTime.setMinWidth(45);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            orderRow.getChildren().addAll(lblId, lblTime, lblTable, lblStatus, spacer, lblTotal);

            cellContainer.getChildren().addAll(lblDaySeparator, orderRow);

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
                // Popola la riga dell'ordine
                lblId.setText("#" + order.getId());
                lblTable.setText("Tavolo " + order.getTavolo());
                lblTotal.setText(String.format("€%.2f", order.getTotale()));
                lblStatus.setText(order.getStatus());

                // Mostra l'orario
                if (order.getDataOra() != null) {
                    lblTime.setText(order.getDataOra().format(TIME_FORMATTER));
                } else {
                    lblTime.setText("--:--");
                }

                // Separatore giornaliero: visibile solo se è il primo ordine
                // di un nuovo giorno rispetto all'elemento precedente nella lista
                boolean showDaySeparator = shouldShowDaySeparator(getIndex(), order);
                lblDaySeparator.setVisible(showDaySeparator);
                lblDaySeparator.setManaged(showDaySeparator);

                if (showDaySeparator && order.getDataOra() != null) {
                    lblDaySeparator.setText(
                            order.getDataOra().toLocalDate().format(DAY_FORMATTER));
                }

                setGraphic(cellContainer);
            }
        }

        /**
         * Determina se l'ordine corrente è il primo di un nuovo giorno
         * confrontandolo con l'ordine precedente nella lista visualizzata.
         */
        private boolean shouldShowDaySeparator(int index, Order currentOrder) {
            if (index <= 0) {
                return true; // Primo elemento: mostra sempre il separatore
            }

            List<Order> items = ordersListView.getItems();
            if (index >= items.size()) {
                return false;
            }

            Order previousOrder = items.get(index - 1);
            if (currentOrder.getDataOra() == null || previousOrder.getDataOra() == null) {
                return currentOrder.getDataOra() != null;
            }

            LocalDate currentDate = currentOrder.getDataOra().toLocalDate();
            LocalDate previousDate = previousOrder.getDataOra().toLocalDate();
            return !currentDate.equals(previousDate);
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