package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.service.DatabaseService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;
import javafx.scene.control.SeparatorMenuItem;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EarningController {

    @FXML private VBox ordersContainer;
    @FXML private VBox detailsPane;
    @FXML private TextField searchField;

    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    private List<Order> allOrders = new ArrayList<>();
    private Map<Integer, List<Order>> ordersByTable;

    // Stato selezione
    private Integer selectedTable = null;
    private HBox selectedCard = null;

    // Timer per auto-aggiornamento
    private Timeline pollingTimeline;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    /* ======================
       INIT
       ====================== */

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (session != null) {
            User u = session.getUser();
            if (u != null) {
                lblHeaderName.setText(u.getUsername());
                lblHeaderRole.setText(u.getRole().toUpperCase());
            }
        }

        if (profileBtn != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }

        // Caricamento iniziale
        loadOpenOrders();

        // Avvio Polling (ogni 5 secondi) per vedere nuovi ordini consegnati
        startPolling();
    }

    private void startPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            // Aggiorna i dati mantenendo (se possibile) la selezione
            refreshDataPreservingSelection();
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    // Chiamare questo metodo se si cambia vista per fermare il timer
    public void stopPolling() {
        if (pollingTimeline != null) pollingTimeline.stop();
    }

    /* ======================
       LOAD DATA
       ====================== */

    private void loadOpenOrders() {
        // Ora chiama la versione aggiornata che cerca 'delivered'
        allOrders = DatabaseService.getOrdersToPay();
        groupOrdersByTable();

        // Applica eventuali filtri di ricerca attivi
        if (searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            onSearch();
        } else {
            renderTableCards(ordersByTable);
        }
    }

    private void refreshDataPreservingSelection() {
        Integer previouslySelected = selectedTable;

        loadOpenOrders(); // Ricarica e renderizza

        // Se c'era un tavolo selezionato ed esiste ancora, ripristina la vista dettagli
        if (previouslySelected != null && ordersByTable.containsKey(previouslySelected)) {
            // (La selezione grafica della card è complessa da ripristinare qui senza riferimenti diretti,
            // ma possiamo riaprire i dettagli per continuità operativa)
            List<Order> orders = ordersByTable.get(previouslySelected);
            double total = orders.stream().mapToDouble(Order::getTotale).sum();

            // Aggiorna solo se i dettagli sono cambiati o per mantenere coerenza
            showTableDetails(previouslySelected, orders, total);

            // Nota: La card nella lista a sinistra perderà l'evidenziazione "verde"
            // fino al prossimo click manuale, a meno di non scorrere i figli di ordersContainer
            // per ritrovarla. Per semplicità, riapriamo solo i dettagli.
        } else if (previouslySelected != null && !ordersByTable.containsKey(previouslySelected)) {
            // Il tavolo è sparito (magari pagato da un altro terminale?), pulisci dettagli
            clearDetailsPane();
            selectedTable = null;
        }
    }

    private void groupOrdersByTable() {
        ordersByTable = allOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getTavolo,
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    /* ======================
       SEARCH
       ====================== */

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            renderTableCards(ordersByTable);
            return;
        }

        Map<Integer, List<Order>> filtered = ordersByTable.entrySet().stream()
                .filter(entry -> String.valueOf(entry.getKey()).contains(query))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        TreeMap::new
                ));

        renderTableCards(filtered);
    }

    /* ======================
       RENDER TABLE CARDS
       ====================== */

    private void renderTableCards(Map<Integer, List<Order>> tables) {
        ordersContainer.getChildren().clear();
        // Reset riferimenti grafici, ma manteniamo selectedTable logico per il refresh
        selectedCard = null;

        if (tables.isEmpty()) {
            Label empty = new Label("Nessun conto in attesa");
            empty.setStyle("-fx-font-size: 16px; -fx-text-fill: #999; -fx-padding: 30; -fx-font-style: italic;");
            ordersContainer.getChildren().add(empty);
            return;
        }

        for (Map.Entry<Integer, List<Order>> entry : tables.entrySet()) {
            int tableNumber = entry.getKey();
            List<Order> ordersForTable = entry.getValue();

            HBox card = createTableCard(tableNumber, ordersForTable);
            ordersContainer.getChildren().add(card);

            // Ripristino evidenziazione visiva se è il tavolo selezionato
            if (selectedTable != null && selectedTable == tableNumber) {
                card.setStyle("-fx-background-color: #E8F8F5; -fx-border-color: #2ecc71; -fx-border-width: 0 0 3 0; -fx-cursor: hand;");
                selectedCard = card;
            }
        }
    }

    private HBox createTableCard(int tableNumber, List<Order> orders) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        String baseStyle = "-fx-background-color: white; -fx-border-color: #DDD; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #F9F9F9; -fx-border-color: #DDD; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        String selectedStyle = "-fx-background-color: #E8F8F5; -fx-border-color: #2ecc71; -fx-border-width: 0 0 3 0; -fx-cursor: hand;";

        // Imposta stile iniziale (se non è già selezionato, logica gestita in renderTableCards)
        if (selectedTable == null || selectedTable != tableNumber) {
            card.setStyle(baseStyle);
        }

        // === SINISTRA: Info Tavolo ===
        VBox leftInfo = new VBox(5);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        Label lblTable = new Label("Tavolo " + tableNumber);
        lblTable.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Info orario primo ordine
        String time = "";
        if(!orders.isEmpty()) {
            time = orders.get(0).getDataOra().format(TIME_FORMATTER);
        }

        Label lblSubtitle = new Label(orders.size() + (orders.size() == 1 ? " ordine" : " ordini") + " - Dal " + time);
        lblSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        leftInfo.getChildren().addAll(lblTable, lblSubtitle);

        // === DESTRA: Totale ===
        double totalAmount = orders.stream().mapToDouble(Order::getTotale).sum();
        Label lblTotal = new Label(String.format("€%.2f", totalAmount));
        lblTotal.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        card.getChildren().addAll(leftInfo, lblTotal);

        // === EVENTI ===
        card.setOnMouseEntered(e -> {
            if (selectedCard != card) card.setStyle(hoverStyle);
        });

        card.setOnMouseExited(e -> {
            if (selectedCard != card) card.setStyle(baseStyle);
        });

        card.setOnMouseClicked(e -> {
            if (selectedCard != null && selectedCard != card) {
                selectedCard.setStyle(baseStyle);
            }
            selectedCard = card;
            selectedTable = tableNumber;
            card.setStyle(selectedStyle);

            showTableDetails(tableNumber, orders, totalAmount);
        });

        return card;
    }

    /* ======================
       DETAILS PANE
       ====================== */

    private void showTableDetails(int tableNumber, List<Order> orders, double totalAmount) {
        detailsPane.getChildren().clear();

        VBox content = new VBox(15);
        content.setPadding(new Insets(0));

        // HEADER
        Label lblTitle = new Label("Tavolo " + tableNumber);
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");
        Separator sep1 = new Separator();

        // LISTA
        VBox ordersBox = new VBox(10);
        for (Order order : orders) {
            ordersBox.getChildren().add(createOrderSection(order));
        }

        ScrollPane scrollOrders = new ScrollPane(ordersBox);
        scrollOrders.setFitToWidth(true);
        scrollOrders.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollOrders, Priority.ALWAYS);

        // TOTALE
        Separator sep2 = new Separator();
        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_RIGHT);

        Label lblTotalLabel = new Label("TOTALE:");
        lblTotalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label lblTotalValue = new Label(String.format("€%.2f", totalAmount));
        lblTotalValue.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2ecc71; -fx-padding: 0 0 0 15;");

        totalBox.getChildren().addAll(lblTotalLabel, lblTotalValue);

        // BOTTONE PAGA
        Button btnPay = new Button("Incassa e Chiudi");
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setPrefHeight(50);
        btnPay.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

        btnPay.setOnAction(e -> markTableAsPaid(orders));

        content.getChildren().addAll(lblTitle, sep1, scrollOrders, sep2, totalBox, btnPay);
        detailsPane.getChildren().add(content);
    }

/* !! deprecato versione aggiornata sotto
    private VBox createOrderSection(Order order) {
        // ... (Implementazione identica alla tua versione precedente) ...
        // Per brevità qui riporto solo la struttura, il contenuto interno è lo stesso
        VBox section = new VBox(8);
        section.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 12; -fx-background-radius: 5; -fx-border-color: #E0E0E0;");

        HBox orderHeader = new HBox(10);
        Label lblOrderId = new Label("Ordine #" + order.getId());
        lblOrderId.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");

        // Label Stato "Consegnato" per chiarezza
        Label lblStatus = new Label("Consegnato");
        lblStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #9E9E9E; -fx-padding: 2 6; -fx-background-radius: 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblOrderTotal = new Label(String.format("€%.2f", order.getTotale()));
        lblOrderTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        orderHeader.getChildren().addAll(lblOrderId, lblStatus, spacer, lblOrderTotal);

        VBox itemsList = new VBox(5);
        List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());
        for (String item : items) {
            itemsList.getChildren().add(new Label("• " + item));
        }

        section.getChildren().addAll(orderHeader, itemsList);
        return section;
    }
*/


    private void clearDetailsPane() {
        detailsPane.getChildren().clear();
        Label placeholder = new Label("Seleziona un tavolo");
        placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #999; -fx-font-style: italic;");
        VBox centerBox = new VBox(placeholder);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerBox, Priority.ALWAYS);
        detailsPane.getChildren().add(centerBox);
    }

    private void markTableAsPaid(List<Order> orders) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Pagamento");
        confirm.setHeaderText("Incassare tavolo?");
        confirm.setContentText("Chiudere " + orders.size() + " ordini per €" + String.format("%.2f", orders.stream().mapToDouble(Order::getTotale).sum()) + "?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean allSuccess = true;
            for (Order order : orders) {
                if (!DatabaseService.markOrderAsPaid(order.getId())) allSuccess = false;
            }

            if (allSuccess) {
                // Non mostriamo popup di successo invasivi, ricarichiamo e basta per velocità operativa
                selectedTable = null;
                selectedCard = null;
                refreshDataPreservingSelection();
                clearDetailsPane();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Errore nel salvataggio del pagamento.");
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void handleProfileMenu(MouseEvent event) {
        ContextMenu contextMenu = new ContextMenu();

        // 1. Voce Cambia Password (Nuova)
        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.setOnAction(e -> {
            // Recupera lo Stage attuale e mostra il dialog
            Stage currentStage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(currentStage);
        });

        // 2. Voce Logout (Migliorata con stile rosso)
        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        itemLogout.setOnAction(e -> {
            stopPolling(); // Importante: ferma il timer
            UserSession.cleanUserSession();
            try {
                // Torna al Login
                Parent loginView = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
                profileBtn.getScene().setRoot(loginView);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Aggiungi tutto al menu con un separatore estetico
        contextMenu.getItems().addAll(itemChangePassword, new SeparatorMenuItem(), itemLogout);

        // Mostra il menu sotto il bottone profilo
        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }


    private VBox createOrderSection(Order order) {
        VBox section = new VBox(8);
        // Stile "card" leggera
        section.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 12; -fx-background-radius: 5; -fx-border-color: #E0E0E0;");

        // === HEADER DELL'ORDINE (ID e Totale parziale) ===
        HBox orderHeader = new HBox(10);
        orderHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblOrderId = new Label("Ordine #" + order.getId());
        lblOrderId.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");

        // Label Stato
        Label lblStatus = new Label("Consegnato");
        lblStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #9E9E9E; -fx-padding: 2 6; -fx-background-radius: 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Spinge il totale a destra

        Label lblOrderTotal = new Label(String.format("€%.2f", order.getTotale()));
        lblOrderTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71; -fx-font-size: 14px;");

        orderHeader.getChildren().addAll(lblOrderId, lblStatus, spacer, lblOrderTotal);

        // === LISTA ARTICOLI CON PREZZI ===
        VBox itemsList = new VBox(4);
        itemsList.setPadding(new Insets(8, 0, 0, 10)); // Indentazione leggera

        // USIAMO IL NUOVO METODO DEL SERVICE
        List<OrderItem> items = DatabaseService.getOrderItemsDetailed(order.getId());

        for (OrderItem item : items) {
            // Calcolo totale riga (Quantità * Prezzo Snapshot)
            double rowTotal = item.getQuantita() * item.getPrezzoSnapshot();

            // Layout Riga: "2x Pizza Margherita ............ € 14.00"
            HBox itemRow = new HBox(10);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            // Nome e Quantità
            String nomeProdotto = item.getProduct() != null ? item.getProduct().getNome() : "???";
            Label lblName = new Label(item.getQuantita() + "x " + nomeProdotto);
            lblName.setStyle("-fx-text-fill: #444; -fx-font-size: 13px;");

            Region itemSpacer = new Region();
            HBox.setHgrow(itemSpacer, Priority.ALWAYS); // Spinge il prezzo a destra

            // Prezzo Riga
            Label lblPrice = new Label(String.format("€%.2f", rowTotal));
            lblPrice.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

            itemRow.getChildren().addAll(lblName, itemSpacer, lblPrice);
            itemsList.getChildren().add(itemRow);
        }

        // Note (se presenti)
        if (order.hasNote()) {
            Label lblNote = new Label("Note: " + order.getNote());
            lblNote.setWrapText(true);
            lblNote.setStyle("-fx-text-fill: #D32F2F; -fx-font-style: italic; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");
            itemsList.getChildren().add(lblNote);
        }

        section.getChildren().addAll(orderHeader, new Separator(), itemsList);
        return section;
    }


}