package com.example.demo.view;

import com.example.demo.model.Order;
import com.example.demo.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.stream.Collectors;

import static com.example.demo.view.LoginController.logger;

public class FinancialController {

    @FXML private VBox ordersContainer;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch; // Riferimento alla barra di ricerca

    // Cache: contiene TUTTI gli ordini scaricati dal DB
    private List<Order> allOrdersMaster = new ArrayList<>();

    @FXML
    public void initialize() {
        setupManageButton();

        // 1. Scarichiamo i dati dal DB una volta sola
        loadDataFromDB();

        // 2. Attiviamo il filtro di ricerca
        setupSearchListener();
    }

    // --- LOGICA RICERCA ---
    private void setupSearchListener() {
        // Aggiunge un listener: ogni volta che il testo cambia, esegue il codice
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterAndRender(newValue);
        });
    }

    private void filterAndRender(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();

        // Se la barra è vuota, mostra tutto
        if (lowerCaseQuery.isEmpty()) {
            renderOrders(allOrdersMaster);
            return;
        }

        // Altrimenti filtra la Master List
        List<Order> filteredList = allOrdersMaster.stream()
                .filter(order -> matchesFilter(order, lowerCaseQuery))
                .collect(Collectors.toList());

        renderOrders(filteredList);
    }

    // Qui definiamo le regole: controlliamo ID, Tavolo, Prezzo e Data
    private boolean matchesFilter(Order order, String query) {
        // Cerca nell'ID
        if (String.valueOf(order.getId()).contains(query)) return true;

        // Cerca nel Tavolo
        if (String.valueOf(order.getTavolo()).contains(query)) return true;

        // Cerca nel Totale (convertito in stringa es: "15.50")
        if (String.valueOf(order.getTotale()).contains(query)) return true;

        // Cerca nella Data (es: cerco "2025" o "12")
        if (order.getDataOra().toString().contains(query)) return true;

        return false;
    }

    // --- CARICAMENTO DATI ---
    private void loadDataFromDB() {
        // Scarichiamo dal DB e salviamo nella lista Master
        allOrdersMaster = DatabaseService.getAllOrdersWithTotal();

        // Disegniamo tutto all'inizio
        renderOrders(allOrdersMaster);
    }

    // Questo metodo si occupa SOLO di disegnare (raggruppare e creare le righe)
    private void renderOrders(List<Order> ordersToRender) {
        ordersContainer.getChildren().clear();

        if (ordersToRender.isEmpty()) {
            Label empty = new Label("Nessun ordine trovato.");
            empty.setStyle("-fx-padding: 20; -fx-text-fill: #888; -fx-font-style: italic;");
            ordersContainer.getChildren().add(empty);
            return;
        }

        // Raggruppamento per Data
        Map<LocalDate, List<Order>> grouped = ordersToRender.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDataOra().toLocalDate(),
                        () -> new TreeMap<>(java.util.Collections.reverseOrder()),
                        Collectors.toList()
                ));

        DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALY);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Map.Entry<LocalDate, List<Order>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<Order> ordersOfDay = entry.getValue();

            // Intestazione Data
            Label dateHeader = new Label(date.format(headerFormatter).toUpperCase());
            dateHeader.setStyle("-fx-font-size: 12px; -fx-text-fill: #999; -fx-font-weight: bold; -fx-padding: 15 0 5 0;");
            ordersContainer.getChildren().add(dateHeader);

            // Righe Ordini
            for (Order order : ordersOfDay) {
                HBox row = createOrderRow(order, timeFormatter);
                ordersContainer.getChildren().add(row);
            }
        }
    }

    // --- METODI DI SUPPORTO (Row Creation & Style) ---
    // (Uguale a prima, solo copiato per completezza)
    private HBox createOrderRow(Order order, DateTimeFormatter timeFormatter) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 0, 15, 0));
        row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white; -fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #F9F9F9; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white; -fx-cursor: hand;"));

        row.setOnMouseClicked(e -> logger.log(Level.INFO, "Apro dettagli ordine #" + order.getId()));



        VBox infoBox = new VBox(4);
        Label title = new Label("Order " + order.getId() + (order.getTavolo() > 0 ? " (Tav. " + order.getTavolo() + ")" : ""));
        title.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

        String subText = String.format("hours: %s; total: %.2f€",
                order.getDataOra().format(timeFormatter),
                order.getTotale());
        Label subtitle = new Label(subText);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");

        infoBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dots = new Label("...");
        dots.setStyle("-fx-font-size: 20px; -fx-text-fill: #555; -fx-padding: 0 10 0 0;");

        row.getChildren().addAll(infoBox, spacer, dots);
        return row;
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
        try {
            ordersContainer.getScene().setRoot(ManagerController.getFXMLView());
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "errore nel tornare alla ManagerView", e);

        }
    }

    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(FinancialController.class.getResource("/FinancialView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare FinancialView.fxml", e);
        }
    }
}