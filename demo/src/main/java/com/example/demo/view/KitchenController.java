package com.example.demo.view;

import com.example.demo.model.Order;
import com.example.demo.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class KitchenController {

    @FXML
    private VBox ordersContainer;

    // Formatter per le date e orari
    private final DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        refreshData();
    }

    public void refreshData() {
        ordersContainer.getChildren().clear();

        // 1. Recupera ordini filtrati (Stato "ordinato" + Ultime 24h) dal DB
        List<Order> activeOrders = DatabaseService.getKitchenActiveOrders();

        if (activeOrders.isEmpty()) {
            Label emptyLabel = new Label("Nessun ordine in attesa.");
            emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 16px; -fx-padding: 20;");
            ordersContainer.getChildren().add(emptyLabel);
            return;
        }

        // 2. Raggruppa per Data (TreeMap reverseOrder mette oggi prima di ieri)
        Map<LocalDate, List<Order>> grouped = activeOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDataOra().toLocalDate(),
                        () -> new TreeMap<>(java.util.Collections.reverseOrder()),
                        Collectors.toList()
                ));

        // 3. Costruisci la UI
        for (Map.Entry<LocalDate, List<Order>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<Order> ordersOfDay = entry.getValue();

            // A. Header Data
            Label dateHeader = new Label(date.format(headerFormatter).toUpperCase());
            dateHeader.setStyle("-fx-font-size: 13px; -fx-text-fill: #666; -fx-font-weight: bold; -fx-padding: 15 0 5 5;");
            ordersContainer.getChildren().add(dateHeader);

            // B. Lista Ordini (Cards)
            for (Order order : ordersOfDay) {
                HBox card = createOrderCard(order);
                ordersContainer.getChildren().add(card);
            }
        }
    }

    private HBox createOrderCard(Order order) {
        HBox card = new HBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setSpacing(15);
        // Stile Card: Bianco, bordi arrotondati, margine inferiore
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #EEE; -fx-border-width: 1;");
        VBox.setMargin(card, new Insets(0, 0, 10, 0));

        // Effetto Ombra
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.05));
        shadow.setRadius(5);
        shadow.setOffsetY(2);
        card.setEffect(shadow);

        // --- COLONNA INFO ---
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Titolo: "Order 53 (Tav. 4)"
        String titleText = "Order " + order.getId() + (order.getTavolo() > 0 ? " (Tav. " + order.getTavolo() + ")" : "");
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Sottotitolo: "ordered at: 10:30 - tot: 15.50€"
        String subTextStr = String.format("ordered at: %s", order.getDataOra().format(timeFormatter));
        // Se vuoi mostrare anche il totale:
        // String subTextStr = String.format("ordered at: %s • tot: %.2f€", order.getDataOra().format(timeFormatter), order.getTotale());

        Label subText = new Label(subTextStr);
        subText.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");

        infoBox.getChildren().addAll(title, subText);

        // --- BADGE DELAYED ---
        // Se l'ordine è più vecchio di 20 minuti, mostriamo "Delayed"
        long minutesAgo = Duration.between(order.getDataOra(), LocalDateTime.now()).toMinutes();
        if (minutesAgo > 20) {
            Label delayedBadge = new Label("Delayed");
            delayedBadge.setStyle("-fx-background-color: #E69500; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 10;");
            card.getChildren().add(infoBox);
            card.getChildren().add(delayedBadge);
        } else {
            card.getChildren().add(infoBox);
        }

        // --- CHECKBOX AZIONE ---
        CheckBox doneCheck = new CheckBox();
        doneCheck.setStyle("-fx-cursor: hand; -fx-font-size: 14px;");

        // Al click: aggiorna DB e rimuove la card
        doneCheck.setOnAction(e -> {
            if (doneCheck.isSelected()) {
                markAsDone(order, card);
            }
        });

        card.getChildren().add(doneCheck);

        return card;
    }

    private void markAsDone(Order order, HBox card) {
        // 1. Aggiorna lo stato nel DB usando il metodo che avevi già
        boolean success = DatabaseService.setOrderStatus(order.getId(), "pronto");

        if (success) {
            System.out.println("Ordine #" + order.getId() + " completato.");
            // 2. Rimuovi visivamente la card
            ordersContainer.getChildren().remove(card);

            // Opzionale: Se vuoi ricaricare tutto per pulire eventuali header vuoti:
            // refreshData();
        } else {
            System.out.println("Errore aggiornamento ordine #" + order.getId());
            // Se fallisce, deseleziona la checkbox
            ((CheckBox) card.getChildren().get(card.getChildren().size() - 1)).setSelected(false);
        }
    }


    public static javafx.scene.Parent getFXMLView() {
        try {
            return new javafx.fxml.FXMLLoader(KitchenController.class.getResource("/Kitchen.fxml")).load();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Impossibile caricare KitchenView.fxml", e);
        }
    }
}