package com.example.demo.view;

import com.example.demo.model.Order;
import com.example.demo.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FinancialController {

    @FXML private VBox ordersContainer;

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        ordersContainer.getChildren().clear();

        // 1. Recupera tutti gli ordini con totale
        List<Order> allOrders = DatabaseService.getAllOrdersWithTotal();

        // 2. Raggruppa per Data (Giorno)
        // Usiamo TreeMap con reverseOrder per avere le date più recenti in alto
        Map<LocalDate, List<Order>> grouped = allOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDataOra().toLocalDate(),
                        () -> new TreeMap<>(java.util.Collections.reverseOrder()),
                        Collectors.toList()
                ));

        // 3. Genera la grafica
        DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"); // es: "Lunedì 10 Ottobre 2025"
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Map.Entry<LocalDate, List<Order>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<Order> ordersOfDay = entry.getValue();

            // --- A. INTESTAZIONE DATA (Sezione) ---
            Label dateHeader = new Label(date.format(headerFormatter).toUpperCase());
            dateHeader.setStyle("-fx-font-size: 12px; -fx-text-fill: #888; -fx-font-weight: bold; -fx-padding: 15 0 5 0;");
            ordersContainer.getChildren().add(dateHeader);

            // --- B. LISTA ORDINI DI QUEL GIORNO ---
            for (Order order : ordersOfDay) {
                HBox row = createOrderRow(order, timeFormatter);
                ordersContainer.getChildren().add(row);
            }
        }
    }

    private HBox createOrderRow(Order order, DateTimeFormatter timeFormatter) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 0, 15, 0));
        // Stile riga (linea sotto)
        row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white; -fx-cursor: hand;");

        // Effetto Hover
        row.setOnMouseEntered(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #F9F9F9; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white; -fx-cursor: hand;"));

        // AZIONE CLICK: Aprirà i dettagli
        row.setOnMouseClicked(e -> {
            System.out.println("Apro dettagli ordine #" + order.getId());
            // TODO: Qui collegheremo la OrderDetailView
        });

        // 1. Info Sinistra (Titolo e Sottotitolo)
        VBox infoBox = new VBox(4);

        // Titolo: "Order 53"
        Label title = new Label("Order " + order.getId() + (order.getTavolo() > 0 ? " (Tav. " + order.getTavolo() + ")" : ""));
        title.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

        // Sottotitolo: "hours: 11:23 PM; total: 11€"
        String subText = String.format("hours: %s; total: %.2f€",
                order.getDataOra().format(timeFormatter),
                order.getTotale());
        Label subtitle = new Label(subText);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");

        infoBox.getChildren().addAll(title, subtitle);

        // 2. Spaziatore
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. I tre puntini (...)
        Label dots = new Label("...");
        dots.setStyle("-fx-font-size: 20px; -fx-text-fill: #555; -fx-padding: 0 10 0 0;");

        row.getChildren().addAll(infoBox, spacer, dots);
        return row;
    }

    @FXML
    private void goBack() {
        // Torna alla dashboard Manager
        ordersContainer.getScene().setRoot(ManagerController.getFXMLView());
    }

    // Helper statico per caricare la vista
    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(FinancialController.class.getResource("/FinancialView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare FinancialView.fxml", e);
        }
    }
}