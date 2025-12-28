package com.example.rm.view;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.LoggerService;
import com.example.rm.view.component.DateHeaderController;
import com.example.rm.view.component.OrderRowController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FinancialController {

    @FXML private VBox ordersContainer;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch;

    private List<Order> allOrdersMaster = new ArrayList<>();

    private static final Logger logger =
            LoggerService.getLogger(FinancialController.class);

    @FXML
    public void initialize() {
        setupManageButton();
        loadDataFromDB();
        setupSearchListener();
    }

    private void setupSearchListener() {
        if (txtSearch == null) {
            logger.log(Level.WARNING,
                    "txtSearch non iniettato correttamente dall'FXML");
            return;
        }

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterAndRender(newValue);
        });
    }

    private void filterAndRender(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();

        // CORRETTO: "è" invece di "Ã¨"
        if (lowerCaseQuery.isEmpty()) {
            renderOrders(allOrdersMaster);
            return;
        }

        List<Order> filteredList = allOrdersMaster.stream()
                .filter(order -> matchesFilter(order, lowerCaseQuery))
                .collect(Collectors.toList());

        renderOrders(filteredList);
    }

    private boolean matchesFilter(Order order, String query) {
        return String.valueOf(order.getId()).contains(query)
                || String.valueOf(order.getTavolo()).contains(query)
                || String.valueOf(order.getTotale()).contains(query)
                || order.getDataOra().toString().contains(query);
    }

    private void loadDataFromDB() {
        allOrdersMaster = DatabaseService.getAllOrdersWithTotal();
        renderOrders(allOrdersMaster);
    }

    private void renderOrders(List<Order> ordersToRender) {
        ordersContainer.getChildren().clear();

        if (ordersToRender.isEmpty()) {
            Label empty = new Label("Nessun ordine trovato.");
            empty.setStyle("-fx-padding: 20; -fx-text-fill: #888; -fx-font-style: italic;");
            ordersContainer.getChildren().add(empty);
            return;
        }

        Map<LocalDate, List<Order>> grouped = ordersToRender.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDataOra().toLocalDate(),
                        () -> new TreeMap<>(java.util.Collections.reverseOrder()),
                        Collectors.toList()
                ));

        DateTimeFormatter headerFormatter =
                DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALY);

        for (var entry : grouped.entrySet()) {
            ordersContainer.getChildren().add(
                    loadDateHeader(
                            entry.getKey()
                                    .format(headerFormatter)
                                    .toUpperCase()
                    )
            );

            for (Order order : entry.getValue()) {
                ordersContainer.getChildren().add(loadOrderRow(order));
            }
        }
    }

    private Parent loadOrderRow(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/OrderRow.fxml")
            );
            Parent root = loader.load();
            OrderRowController controller = loader.getController();
            controller.setOrder(order, null);
            return root;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento OrderRow.fxml", e);
            return new Label("Errore caricamento ordine");
        }
    }

    private void setupManageButton() {
        if (lblManage == null) return;

        String styleNormal = "-fx-text-fill: #888888; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: false;";
        String styleHover  = "-fx-text-fill: #333333; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: true;";

        lblManage.setStyle(styleNormal);
        lblManage.setOnMouseEntered(e -> lblManage.setStyle(styleHover));
        lblManage.setOnMouseExited(e -> lblManage.setStyle(styleNormal));
    }

    @FXML
    private void goBack() {
        logger.log(Level.INFO, "invocazione goBack");
        View managerView = ViewFactory.forRole("manager");
        ordersContainer.getScene().setRoot(managerView.getRoot());
    }

    private Parent loadDateHeader(String text) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/DateHeader.fxml")
            );
            Parent root = loader.load();
            DateHeaderController controller = loader.getController();
            controller.setDateText(text);
            return root;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento DateHeader.fxml", e);
            return new Label(text);
        }
    }
}