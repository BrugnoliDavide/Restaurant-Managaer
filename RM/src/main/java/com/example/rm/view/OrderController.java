package com.example.rm.view;

import com.example.rm.controller.OrderService;
import com.example.rm.controller.OrderUseCase;
import com.example.rm.model.Order;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;


public class OrderController {

    private static final Logger logger = Logger.getLogger(OrderController.class.getName());

    // Iniezione stile Menu/Kitchen dialog (default concreto, ma sostituibile)
    private static OrderUseCase orderUseCase = new OrderService();
    public static void setOrderUseCase(OrderUseCase useCase) { orderUseCase = useCase; }

    @FXML private Label lblManage;
    @FXML private TextField txtSearch;

    @FXML private TableView<Order> tblOrders;
    @FXML private TableColumn<Order, Number> colId;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, Number> colTable;
    @FXML private TableColumn<Order, String> colUser;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Number> colTotal;
    @FXML private TableColumn<Order, String> colNote;

    @FXML private Button btnRefresh;
    @FXML private Button btnReady;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;

    private FilteredList<Order> filtered;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadOrders();
        setupSearch();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> data.getValue().idProperty());
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDataOraFormatted()));
        colTable.setCellValueFactory(data -> data.getValue().tavoloProperty());
        colUser.setCellValueFactory(data -> data.getValue().usernameProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colTotal.setCellValueFactory(data -> data.getValue().totaleProperty());colNote.setCellValueFactory(data -> data.getValue().noteProperty());
    }

    private void loadOrders() {
        try {
            List<Order> orders = orderUseCase.loadAllOrders();
            filtered = new FilteredList<>(FXCollections.observableArrayList(orders), o -> true);
            tblOrders.setItems(filtered);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento ordini", e);
            filtered = new FilteredList<>(FXCollections.observableArrayList(), o -> true);
            tblOrders.setItems(filtered);
        }
    }

    private void setupSearch() {
        if (txtSearch == null) return;

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase();
            filtered.setPredicate(o -> {
                if (q.isEmpty()) return true;
                boolean matchTable = String.valueOf(o.getTavolo()).contains(q);
                boolean matchUser = o.getUsername() != null && o.getUsername().toLowerCase().contains(q);
                return matchTable || matchUser;
            });
        });
    }

    private Order selectedOrderOrWarn() {
        Order sel = tblOrders.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Seleziona un ordine prima.", ButtonType.OK);
            a.showAndWait();
        }
        return sel;
    }

    @FXML
    private void onRefresh() {
        loadOrders();
    }

    @FXML
    private void onMarkReady() {
        Order sel = selectedOrderOrWarn();
        if (sel == null) return;
        orderUseCase.markAsReady(sel.getId());
        loadOrders();
    }

    @FXML
    private void onCloseOrder() {
        Order sel = selectedOrderOrWarn();
        if (sel == null) return;
        orderUseCase.closeOrder(sel.getId());
        loadOrders();
    }

    @FXML
    private void onCancelOrder() {
        Order sel = selectedOrderOrWarn();
        if (sel == null) return;
        orderUseCase.cancelOrder(sel.getId());
        loadOrders();
    }

    @FXML
    private void goBack() {
        // Copia/incolla la stessa logica che hai in MenuController.goBack()
        // (ViewFactory.forRole("manager") + setRoot(...))
    }
}
