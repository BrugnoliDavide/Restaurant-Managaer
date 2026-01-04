package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.Order;
import com.example.rm.model.User;
import com.example.rm.service.DatabaseService;
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

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;

/**
 * Controller migliorato per EarningView (Cassa)
 *
 * CARATTERISTICHE:
 * - Raggruppa ordini per tavolo
 * - Mostra totale complessivo per tavolo
 * - Lista dettagliata articoli nella sezione destra
 * - Ricerca per numero tavolo
 * - Selezione visiva
 */
public class EarningController {

    @FXML private VBox ordersContainer;
    @FXML private VBox detailsPane;
    @FXML private TextField searchField;

    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    /* ======================
       STATE
       ====================== */

    // Lista completa ordini dal database
    private List<Order> allOrders = new ArrayList<>();

    // Mappa: numero tavolo → lista ordini
    private Map<Integer, List<Order>> ordersByTable;

    // Tavolo attualmente selezionato
    private Integer selectedTable;

    // Card attualmente selezionata (per highlight)
    private HBox selectedCard;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    /* ======================
       INIT
       ====================== */

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        User u = session.getUser();

        lblHeaderName.setText(u.getUsername());
        lblHeaderRole.setText(u.getRole().toUpperCase());

        profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
        profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));

        loadOpenOrders();
    }

    /* ======================
       LOAD DATA
       ====================== */

    /**
     * Carica gli ordini pronti per il pagamento dal database
     */
    private void loadOpenOrders() {
        allOrders = DatabaseService.getOrdersToPay();
        groupOrdersByTable();
        renderTableCards(ordersByTable);
        clearDetailsPane();
    }

    /**
     * Raggruppa gli ordini per numero di tavolo
     */
    private void groupOrdersByTable() {
        ordersByTable = allOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getTavolo,
                        TreeMap::new, // TreeMap per ordinamento automatico per tavolo
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
            // Mostra tutti i tavoli
            renderTableCards(ordersByTable);
            return;
        }

        // Filtra per numero tavolo
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

    /**
     * Renderizza le card dei tavoli con totale raggruppato
     */
    private void renderTableCards(Map<Integer, List<Order>> tables) {
        ordersContainer.getChildren().clear();
        selectedCard = null;
        selectedTable = null;

        if (tables.isEmpty()) {
            Label empty = new Label("Nessun ordine da pagare");
            empty.setStyle(
                    "-fx-font-size: 16px; " +
                            "-fx-text-fill: #999; " +
                            "-fx-padding: 30; " +
                            "-fx-font-style: italic;"
            );
            ordersContainer.getChildren().add(empty);
            return;
        }

        for (Map.Entry<Integer, List<Order>> entry : tables.entrySet()) {
            int tableNumber = entry.getKey();
            List<Order> ordersForTable = entry.getValue();

            HBox card = createTableCard(tableNumber, ordersForTable);
            ordersContainer.getChildren().add(card);
        }
    }

    /**
     * Crea una card per un tavolo con totale e numero ordini
     */
    private HBox createTableCard(int tableNumber, List<Order> orders) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        // Stile base
        String baseStyle =
                "-fx-background-color: white; " +
                        "-fx-border-color: #DDD; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-cursor: hand;";

        String hoverStyle =
                "-fx-background-color: #F9F9F9; " +
                        "-fx-border-color: #DDD; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-cursor: hand;";

        String selectedStyle =
                "-fx-background-color: #E8F8F5; " +
                        "-fx-border-color: #2ecc71; " +
                        "-fx-border-width: 0 0 3 0; " +
                        "-fx-cursor: hand;";

        card.setStyle(baseStyle);

        // === SINISTRA: Info Tavolo ===
        VBox leftInfo = new VBox(5);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        Label lblTable = new Label("Tavolo " + tableNumber);
        lblTable.setStyle(
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        // Numero ordini e orario primo ordine
        Order firstOrder = orders.get(0);
        String time = firstOrder.getDataOra().format(TIME_FORMATTER);

        String subtitle = String.format(
                "%d ordine%s - Dal %s",
                orders.size(),
                orders.size() > 1 ? "i" : "",
                time
        );

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-text-fill: #666;"
        );

        leftInfo.getChildren().addAll(lblTable, lblSubtitle);

        // === DESTRA: Totale ===
        double totalAmount = orders.stream()
                .mapToDouble(Order::getTotale)
                .sum();

        Label lblTotal = new Label(String.format("€%.2f", totalAmount));
        lblTotal.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2ecc71;"
        );

        card.getChildren().addAll(leftInfo, lblTotal);

        // === EVENTI ===
        card.setOnMouseEntered(e -> {
            if (selectedCard != card) {
                card.setStyle(hoverStyle);
            }
        });

        card.setOnMouseExited(e -> {
            if (selectedCard != card) {
                card.setStyle(baseStyle);
            }
        });

        card.setOnMouseClicked(e -> {
            // Deseleziona card precedente
            if (selectedCard != null) {
                selectedCard.setStyle(baseStyle);
            }

            // Seleziona nuova card
            selectedCard = card;
            card.setStyle(selectedStyle);
            selectedTable = tableNumber;

            // Mostra dettagli
            showTableDetails(tableNumber, orders, totalAmount);
        });

        return card;
    }

    /* ======================
       DETAILS PANE
       ====================== */

    /**
     * Mostra i dettagli completi degli ordini del tavolo selezionato
     */
    private void showTableDetails(int tableNumber, List<Order> orders, double totalAmount) {
        detailsPane.getChildren().clear();

        VBox content = new VBox(15);
        content.setPadding(new Insets(0));

        // === HEADER ===
        Label lblTitle = new Label("Tavolo " + tableNumber);
        lblTitle.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        Separator sep1 = new Separator();

        // === LISTA ORDINI E ARTICOLI ===
        VBox ordersBox = new VBox(10);

        for (Order order : orders) {
            VBox orderSection = createOrderSection(order);
            ordersBox.getChildren().add(orderSection);
        }

        ScrollPane scrollOrders = new ScrollPane(ordersBox);
        scrollOrders.setFitToWidth(true);
        scrollOrders.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollOrders, Priority.ALWAYS);

        // === TOTALE FINALE ===
        Separator sep2 = new Separator();

        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_RIGHT);

        Label lblTotalLabel = new Label("TOTALE:");
        lblTotalLabel.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        Label lblTotalValue = new Label(String.format("€%.2f", totalAmount));
        lblTotalValue.setStyle(
                "-fx-font-size: 26px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2ecc71; " +
                        "-fx-padding: 0 0 0 15;"
        );

        totalBox.getChildren().addAll(lblTotalLabel, lblTotalValue);

        // === PULSANTE PAGAMENTO ===
        Button btnPay = new Button("Segna come Pagato");
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setPrefHeight(50);
        btnPay.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );

        btnPay.setOnMouseEntered(e ->
                btnPay.setStyle(btnPay.getStyle() + "-fx-background-color: #45a049;")
        );
        btnPay.setOnMouseExited(e ->
                btnPay.setStyle(btnPay.getStyle().replace("-fx-background-color: #45a049;", ""))
        );

        btnPay.setOnAction(e -> markTableAsPaid(orders));

        // === ASSEMBLAGGIO ===
        content.getChildren().addAll(
                lblTitle,
                sep1,
                scrollOrders,
                sep2,
                totalBox,
                btnPay
        );

        detailsPane.getChildren().add(content);
    }

    /**
     * Crea una sezione per un singolo ordine con i suoi articoli
     */
    private VBox createOrderSection(Order order) {
        VBox section = new VBox(8);
        section.setStyle(
                "-fx-background-color: #FAFAFA; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-radius: 5;"
        );

        // === HEADER ORDINE ===
        HBox orderHeader = new HBox(10);
        orderHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblOrderId = new Label("Ordine #" + order.getId());
        lblOrderId.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2196F3;"
        );

        String time = order.getDataOra().format(TIME_FORMATTER);
        Label lblTime = new Label(time);
        lblTime.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #666;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblOrderTotal = new Label(String.format("€%.2f", order.getTotale()));
        lblOrderTotal.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2ecc71;"
        );

        orderHeader.getChildren().addAll(
                lblOrderId,
                lblTime,
                spacer,
                lblOrderTotal
        );

        // === LISTA ARTICOLI ===
        VBox itemsList = new VBox(5);
        itemsList.setPadding(new Insets(8, 0, 0, 0));

        List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());

        for (String item : items) {
            Label lblItem = new Label("• " + item);
            lblItem.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-text-fill: #444;"
            );
            itemsList.getChildren().add(lblItem);
        }

        // === NOTE (se presenti) ===
        if (order.hasNote()) {
            Label lblNote = new Label("Note: " + order.getNote());
            lblNote.setWrapText(true);
            lblNote.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: #D32F2F; " +
                            "-fx-font-style: italic; " +
                            "-fx-padding: 5 0 0 0;"
            );
            itemsList.getChildren().add(lblNote);
        }

        section.getChildren().addAll(orderHeader, itemsList);

        return section;
    }

    /**
     * Pulisce il pannello dettagli
     */
    private void clearDetailsPane() {
        detailsPane.getChildren().clear();

        Label placeholder = new Label("Seleziona un tavolo");
        placeholder.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-text-fill: #999; " +
                        "-fx-font-style: italic;"
        );

        VBox centerBox = new VBox(placeholder);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        detailsPane.getChildren().add(centerBox);
    }

    /* ======================
       ACTIONS
       ====================== */

    /**
     * Marca tutti gli ordini di un tavolo come pagati
     */
    private void markTableAsPaid(List<Order> orders) {
        // Conferma prima di procedere
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Pagamento");
        confirm.setHeaderText("Segnare tavolo come pagato?");
        confirm.setContentText(
                String.format(
                        "Verranno chiusi %d ordine/i per un totale di €%.2f",
                        orders.size(),
                        orders.stream().mapToDouble(Order::getTotale).sum()
                )
        );

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Marca tutti gli ordini come pagati
            boolean allSuccess = true;

            for (Order order : orders) {
                boolean success = DatabaseService.markOrderAsPaid(order.getId());
                if (!success) {
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                // Mostra conferma
                showSuccessNotification(orders.size());

                // Ricarica dati
                selectedTable = null;
                selectedCard = null;
                loadOpenOrders();
            } else {
                // Errore
                showErrorAlert("Errore durante il pagamento. Riprova.");
            }
        }
    }

    /**
     * Mostra notifica di successo
     */
    private void showSuccessNotification(int orderCount) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pagamento Completato");
        alert.setHeaderText("✓ Operazione riuscita");
        alert.setContentText(
                String.format(
                        "Sono stati chiusi %d ordine/i con successo.",
                        orderCount
                )
        );
        alert.showAndWait();
    }

    /**
     * Mostra alert di errore
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText("Operazione fallita");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ======================
       PROFILE MENU
       ====================== */

    @FXML
    private void handleProfileMenu(MouseEvent event) {
        ContextMenu menu = new ContextMenu();

        // === OPZIONE 1: CAMBIA PASSWORD ===
        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: #2196F3;"
        );
        itemChangePassword.setOnAction(e -> {
            Stage stage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(stage);
        });

        // === OPZIONE 2: LOGOUT ===
        MenuItem logout = new MenuItem("Logout");
        logout.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: red; " +
                        "-fx-font-weight: bold;"
        );
        logout.setOnAction(e -> {
            UserSession.cleanUserSession();

            try {
                Parent login = FXMLLoader.load(
                        getClass().getResource("/LoginView.fxml")
                );
                profileBtn.getScene().setRoot(login);

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // === ASSEMBLA MENU ===
        menu.getItems().addAll(
                itemChangePassword,
                new SeparatorMenuItem(),
                logout
        );

        menu.show(profileBtn, Side.BOTTOM, 0, 0);
    }
}
