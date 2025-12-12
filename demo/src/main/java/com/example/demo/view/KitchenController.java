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
        card.setAlignment(Pos.TOP_LEFT); // Allineato in alto
        card.setPadding(new Insets(15));
        card.setSpacing(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #EEE; -fx-border-width: 1;");
        VBox.setMargin(card, new Insets(0, 0, 10, 0));

        // Effetto Ombra
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.05));
        shadow.setRadius(5);
        shadow.setOffsetY(2);
        card.setEffect(shadow);

        // --- COLONNA INFO (Sinistra) ---
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // 1. HEADER CARD: Titolo e Orario
        String titleText = "Order #" + order.getId() + (order.getTavolo() > 0 ? " (Tav. " + order.getTavolo() + ")" : "");
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2B2B2B;");

        Label timeLbl = new Label("Ore: " + order.getDataOra().format(timeFormatter));
        timeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        infoBox.getChildren().addAll(title, timeLbl);

        // Separatore visivo
        Region line = new Region();
        line.setStyle("-fx-background-color: #F0F0F0; -fx-min-height: 1; -fx-max-height: 1;");
        VBox.setMargin(line, new Insets(5, 0, 5, 0));
        infoBox.getChildren().add(line);

        // 2. LISTA PRODOTTI (Recuperata dal DB)
        List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());

        for (String itemStr : items) {
            // Esempio stringa: "2x Carbonara"
            Label itemLbl = new Label("• " + itemStr);
            // Stile: Grandicello e leggibile per il cuoco
            itemLbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #333; -fx-font-weight: bold;");
            infoBox.getChildren().add(itemLbl);
        }

        // 3. NOTE (Se presenti)
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            VBox noteBox = new VBox(2);
            noteBox.setPadding(new Insets(10, 0, 0, 0)); // Spazio sopra

            Label noteTitle = new Label("NOTE:");
            noteTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #E02E2E; -fx-font-weight: bold;"); // Rosso

            Label noteContent = new Label(order.getNote());
            noteContent.setStyle("-fx-font-size: 14px; -fx-text-fill: #E02E2E; -fx-font-style: italic;"); // Rosso italico

            noteBox.getChildren().addAll(noteTitle, noteContent);
            infoBox.getChildren().add(noteBox);
        }

        // --- COLONNA DESTRA (Azioni / Badge) ---
        VBox rightBox = new VBox(10);
        rightBox.setAlignment(Pos.TOP_RIGHT);

        // Badge Delayed
        long minutesAgo = Duration.between(order.getDataOra(), LocalDateTime.now()).toMinutes();
        if (minutesAgo > 20) {
            Label delayedBadge = new Label("In Ritardo (" + minutesAgo + "m)");
            delayedBadge.setStyle("-fx-background-color: #E69500; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
            rightBox.getChildren().add(delayedBadge);
        }

        // Checkbox "Pronto"
        CheckBox doneCheck = new CheckBox("PRONTO");
        doneCheck.setStyle("-fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #28a745;");
        doneCheck.setOnAction(e -> {
            if (doneCheck.isSelected()) {
                markAsDone(order, card);
            }
        });

        rightBox.getChildren().add(doneCheck);

        // Assemblaggio Card
        card.getChildren().addAll(infoBox, rightBox);

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