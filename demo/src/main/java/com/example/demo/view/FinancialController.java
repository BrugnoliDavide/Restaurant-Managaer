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

    // Iniettiamo la label definita nell'FXML
    @FXML private Label lblManage;

    @FXML
    public void initialize() {
        // Configura l'effetto hover sul tasto indietro
        setupManageButton();

        // Carica i dati
        loadData();
    }

    // --- NUOVO METODO PER L'EFFETTO HOVER ---
    private void setupManageButton() {
        if (lblManage == null) return;

        // Definizione stili
        String styleNormal = "-fx-text-fill: #888888; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: false;";
        String styleHover  = "-fx-text-fill: #333333; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: true;";

        // Applica stile iniziale
        lblManage.setStyle(styleNormal);

        // Listener per il Mouse: Entrata -> Scuro e Sottolineato
        lblManage.setOnMouseEntered(e -> lblManage.setStyle(styleHover));

        // Listener per il Mouse: Uscita -> Grigio e Normale
        lblManage.setOnMouseExited(e -> lblManage.setStyle(styleNormal));
    }

    private void loadData() {
        ordersContainer.getChildren().clear();

        List<Order> allOrders = DatabaseService.getAllOrdersWithTotal();

        Map<LocalDate, List<Order>> grouped = allOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDataOra().toLocalDate(),
                        () -> new TreeMap<>(java.util.Collections.reverseOrder()),
                        Collectors.toList()
                ));

        DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Map.Entry<LocalDate, List<Order>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<Order> ordersOfDay = entry.getValue();

            // Intestazione Data
            Label dateHeader = new Label(date.format(headerFormatter).toUpperCase());
            dateHeader.setStyle("-fx-font-size: 12px; -fx-text-fill: #999; -fx-font-weight: bold; -fx-padding: 15 0 5 0;");
            ordersContainer.getChildren().add(dateHeader);

            // Lista ordini
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
        row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white; -fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #F9F9F9; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white; -fx-cursor: hand;"));

        row.setOnMouseClicked(e -> {
            System.out.println("Apro dettagli ordine #" + order.getId());
        });

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

    @FXML
    private void goBack() {
        // Torna alla dashboard Manager usando il metodo statico
        try {
            ordersContainer.getScene().setRoot(ManagerController.getFXMLView());
        } catch (Exception e) {
            e.printStackTrace();
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