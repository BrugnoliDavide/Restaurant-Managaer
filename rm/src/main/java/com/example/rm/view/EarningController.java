package com.example.rm.view;

import com.example.rm.app.SceneManager;
import com.example.rm.app.UserSession;
import com.example.rm.controller.EarningService;
import com.example.rm.controller.EarningUseCase;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.service.OrderService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.animation.Animation;

public class EarningController {

    private static final Logger logger = Logger.getLogger(EarningController.class.getName());

    @FXML private VBox ordersContainer;
    @FXML private VBox detailsPane;
    @FXML private TextField searchField;
    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    private List<Order> allOrders = new ArrayList<>();
    private Map<Integer, List<Order>> ordersByTable;

    private Integer selectedTable = null;
    private HBox selectedCard = null;

    private Timeline pollingTimeline;
    
    private static final  String oipSTR = "order-item-price";
    private static final  String oinSTR = "order-item-name";
    private static final  String canceledSTR = "canceled";
    private static final  String annullaSTR = "annulla";

    private String formatCurrency(BigDecimal amount) {
        return "€" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    private static final  String selectedString = "selected";
    private static final  String tavoloString = "Tavolo ";
    
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static EarningUseCase earningUseCase = new EarningService();

    public static void setEarningUseCase(EarningUseCase useCase) {
        earningUseCase = useCase;
    }

    private Set<Integer> tablesWithPending = new HashSet<>();

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

        loadOpenOrders();
        startPolling();
    }

    private void startPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5),
                e ->  refreshDataPreservingSelection()));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    public void stopPolling() {
        if (pollingTimeline != null) pollingTimeline.stop();
    }

    private void loadOpenOrders() {
        allOrders = earningUseCase.loadOrdersToPay();
        tablesWithPending = OrderService.getTablesWithPendingOrders();
        groupOrdersByTable();

        if (searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            onSearch();
        } else {
            renderTableCards(ordersByTable);
        }
    }

    private void refreshDataPreservingSelection() {
        Integer previouslySelected = selectedTable;

        loadOpenOrders();

        if (previouslySelected != null && ordersByTable.containsKey(previouslySelected)) {
            List<Order> orders = ordersByTable.get(previouslySelected);
            BigDecimal total = orders.stream()
                .map(Order::getTotale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            showTableDetails(previouslySelected, orders, total);
        } else if (previouslySelected != null && !ordersByTable.containsKey(previouslySelected)) {
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

    private void renderTableCards(Map<Integer, List<Order>> tables) {
        ordersContainer.getChildren().clear();
        selectedCard = null;

        if (tables.isEmpty()) {
            Label empty = new Label("Nessun conto in attesa");
            empty.getStyleClass().add("earning-empty-state");
            ordersContainer.getChildren().add(empty);
            return;
        }

        for (Map.Entry<Integer, List<Order>> entry : tables.entrySet()) {
            int tableNumber = entry.getKey();
            List<Order> ordersForTable = entry.getValue();

            HBox card = createTableCard(tableNumber, ordersForTable);
            ordersContainer.getChildren().add(card);

            if (selectedTable != null && selectedTable == tableNumber) {
                card.getStyleClass().add(selectedString);
                selectedCard = card;
            }
        }
    }

    private HBox createTableCard(int tableNumber, List<Order> orders) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("table-card");

        VBox leftInfo = new VBox(5);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        Label lblTable = new Label(tavoloString + tableNumber);
        lblTable.getStyleClass().add("table-title");

        String time = "";
        if(!orders.isEmpty()) {
            time = orders.get(0).getDataOra().format(TIME_FORMATTER);
        }

        Label lblSubtitle = new Label(orders.size() + (orders.size() == 1 ? " ordine" : " ordini") + " - Dal " + time);
        lblSubtitle.getStyleClass().add("table-subtitle");

        leftInfo.getChildren().addAll(lblTable, lblSubtitle);

        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Label lblTotal = new Label(formatCurrency(totalAmount));
        lblTotal.getStyleClass().add("table-total");

        card.getChildren().addAll(leftInfo, lblTotal);

        if (tablesWithPending.contains(tableNumber)) {
            Label warning = new Label("⚠ Ordine non completo");
            warning.getStyleClass().add("warning-pending");
            leftInfo.getChildren().add(warning);
        }

        card.setOnMouseClicked(e -> {
            if (selectedCard != null && selectedCard != card) {
                selectedCard.getStyleClass().remove(selectedString);
            }
            selectedCard = card;
            selectedTable = tableNumber;
            card.getStyleClass().add(selectedString);

            showTableDetails(tableNumber, orders, totalAmount);
        });

        return card;
    }

    private void showTableDetails(int tableNumber, List<Order> orders, BigDecimal totalAmount) {
        detailsPane.getChildren().clear();

        VBox content = new VBox(15);
        content.setPadding(new Insets(0));

        Label lblTitle = new Label(tavoloString + tableNumber);
        lblTitle.getStyleClass().add("details-title");

        Separator sep1 = new Separator();



        VBox ordersBox = new VBox(10);

        VBox pendingSection = createPendingItemsSection(tableNumber);
        if (!pendingSection.getChildren().isEmpty()) {
            ordersBox.getChildren().add(pendingSection);
        }
        for (Order order : orders) {
            ordersBox.getChildren().add(createOrderSection(order));

        }

        ScrollPane scrollOrders = new ScrollPane(ordersBox);
        scrollOrders.setFitToWidth(true);
        scrollOrders.getStyleClass().add("details-scroll");
        VBox.setVgrow(scrollOrders, Priority.ALWAYS);

        Separator sep2 = new Separator();

        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.getStyleClass().add("details-total-box");

        Label lblTotalLabel = new Label("TOTALE:");
        lblTotalLabel.getStyleClass().add("details-total-label");

        Label lblTotalValue = new Label(formatCurrency(totalAmount));
        lblTotalValue.getStyleClass().add("details-total-value");

        totalBox.getChildren().addAll(lblTotalLabel, lblTotalValue);

        Button btnPay = new Button("Incassa e Chiudi");
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.getStyleClass().add("btn-pay");

        btnPay.setOnAction(e -> markTableAsPaid(orders));

        content.getChildren().addAll(lblTitle, sep1, scrollOrders, sep2, totalBox, btnPay);
        detailsPane.getChildren().add(content);
    }

    private void clearDetailsPane() {
        detailsPane.getChildren().clear();
        Label placeholder = new Label("Seleziona un tavolo");
        placeholder.getStyleClass().add("details-placeholder");
        VBox centerBox = new VBox(placeholder);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerBox, Priority.ALWAYS);
        detailsPane.getChildren().add(centerBox);
    }

    private void markTableAsPaid(List<Order> deliveredOrdersShown) {
        int tavolo = deliveredOrdersShown.get(0).getTavolo();

        List<Integer> pendingOrderIds = earningUseCase.getPendingOrderIds(tavolo);

        if (!pendingOrderIds.isEmpty()) {
            StringBuilder articoli = new StringBuilder();

            int maxItems = Math.min(3, pendingOrderIds.size());
            for (int i = 0; i < maxItems; i++) {
                int id = pendingOrderIds.get(i);
                List<OrderItem> items = earningUseCase.getOrderItemsDetailed(id);

                articoli.append(
                        items.stream()
                                .map(it -> it.getQuantita() + "x " + it.getProduct().getNome())
                                .collect(Collectors.joining(", "))
                );

                if (i < maxItems - 1) {
                    articoli.append("\n");
                }
            }


            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("⚠️ " + pendingOrderIds.size() + " ordini pendenti");
            alert.setHeaderText(tavoloString + tavolo + " ha ordini non consegnati:");
            alert.getDialogPane().setContentText(articoli.toString());


            ButtonType annullaPaga = new ButtonType("Paga e cancella i pendenti");
            ButtonType deliveredPaga = new ButtonType("Contrassegna come consegnato e Paga");
            ButtonType nonPagare = new ButtonType(annullaSTR);
            alert.getButtonTypes().setAll(annullaPaga, deliveredPaga, nonPagare);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == nonPagare) {
                return;
            }

            if (result.get() == annullaPaga) {
                pendingOrderIds.forEach(id -> earningUseCase.setOrderStatus(id, canceledSTR));
                logger.log(Level.INFO,"Annullati pendenti tavolo {0}", tavolo);
            } else if (result.get() == deliveredPaga) {
                pendingOrderIds.forEach(id -> earningUseCase.setOrderStatus(id, "delivered"));
                logger.log(Level.INFO,"Segnati come delivered pendenti tavolo {0}",  tavolo);
                List<Order> allDeliveredForTable = OrderService.getToPay().stream()
                        .filter(o -> o.getTavolo() == tavolo)
                        .toList();
                deliveredOrdersShown = allDeliveredForTable;
            }
        }

        BigDecimal totale = deliveredOrdersShown.stream()
                .map(Order::getTotale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION);
        conferma.setTitle("Conferma Pagamento");
        conferma.setContentText("Incassare " + formatCurrency(totale) + "?");

        if (conferma.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            boolean success = deliveredOrdersShown.stream()
                    .allMatch(o -> earningUseCase.markOrderAsPaid(o.getId()));

            if (success) {
                selectedTable = null;
                selectedCard = null;
                refreshDataPreservingSelection();
                clearDetailsPane();
            }
        }
    }

    @FXML
    private void handleProfileMenu(MouseEvent event) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.setOnAction(e -> {
            Stage currentStage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(currentStage);
        });

        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.getStyleClass().add("context-menu-item-danger");
        itemLogout.setOnAction(e -> {
            stopPolling();
            UserSession.cleanUserSession();
            try {
                SceneManager.showLogin();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "impossibile tornare indietro", ex);
            }
        });

        contextMenu.getItems().addAll(itemChangePassword, new SeparatorMenuItem(), itemLogout);
        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }

    private VBox createOrderSection(Order order) {
        VBox section = new VBox(8);
        section.getStyleClass().add("order-section");

        // HEADER ORDINE
        HBox orderHeader = new HBox(10);
        orderHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblOrderId = new Label("Ordine #" + order.getId());
        lblOrderId.getStyleClass().add("order-header-title");

        boolean isDelivered = "delivered".equalsIgnoreCase(order.getStatus());

        Label lblStatus = new Label(isDelivered ? "Consegnato" : "⏳ In attesa");
        lblStatus.getStyleClass().add("order-status");
        if (!isDelivered) {
            lblStatus.getStyleClass().add("pending-status");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblOrderTotal = new Label(formatCurrency(order.getTotale()));
        lblOrderTotal.getStyleClass().add("order-row-total");

        // ===== BOTTONE ELIMINA ORDINE =====
        Button btnDeleteOrder = new Button("✖");
        btnDeleteOrder.getStyleClass().add("btn-delete-order");

        btnDeleteOrder.setOnAction(e -> {
            earningUseCase.setOrderStatus(order.getId(), canceledSTR);
            refreshDataPreservingSelection();
        });

        orderHeader.getChildren().addAll(
                lblOrderId,
                lblStatus,
                spacer,
                lblOrderTotal,
                btnDeleteOrder
        );

        // ===== LISTA PRODOTTI =====
        VBox itemsList = new VBox(4);
        itemsList.setPadding(new Insets(8, 0, 0, 10));

        List<OrderItem> items = OrderService.getItemsDetailed(order.getId());

        for (OrderItem item : items) {
            BigDecimal rowTotal = item.getPrezzoSnapshot()
                    .multiply(BigDecimal.valueOf(item.getQuantita()));

            HBox itemRow = new HBox(10);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            String nomeProdotto = item.getProduct() != null
                    ? item.getProduct().getNome()
                    : "???";

            Label lblName = new Label(item.getQuantita() + "x " + nomeProdotto);
            lblName.getStyleClass().add(oinSTR);

            Region itemSpacer = new Region();
            HBox.setHgrow(itemSpacer, Priority.ALWAYS);

            Label lblPrice = new Label(formatCurrency(rowTotal));
            lblPrice.getStyleClass().add(oipSTR);

            // BOTTONE ELIMINA ITEM
            Button btnRemoveItem = new Button("✖");
            btnRemoveItem.getStyleClass().add("btn-delete-item");

            btnRemoveItem.setOnAction(e -> {
                String nomeProdottoConferma = item.getProduct() != null
                        ? item.getProduct().getNome()
                        : "articolo";

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Conferma eliminazione");
                confirm.setHeaderText("Eliminare questo articolo dal conto?");
                confirm.setContentText(item.getQuantita() + "x " + nomeProdottoConferma
                        + " dall'ordine #" + order.getId());

                ButtonType btnOk = new ButtonType("Elimina", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnCancel = new ButtonType(annullaSTR, ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(btnOk, btnCancel);

                confirm.showAndWait()
                        .filter(btnOk::equals)
                        .ifPresent(r -> {
                            earningUseCase.cancelItemFromOrder(order.getId(), item.getId());
                            refreshDataPreservingSelection();
                        });
            });

            itemRow.getChildren().addAll(
                    lblName,
                    itemSpacer,
                    lblPrice,
                    btnRemoveItem
            );

            itemsList.getChildren().add(itemRow);
        }

        // ===== NOTE =====
        if (order.hasNote()) {
            Label lblNote = new Label("Note: " + order.getNote());
            lblNote.setWrapText(true);
            lblNote.getStyleClass().add("order-note");
            itemsList.getChildren().add(lblNote);
        }

        section.getChildren().addAll(orderHeader, new Separator(), itemsList);
        return section;
    }

    private VBox createPendingItemsSection(int tableNumber) {
        List<Integer> pendingIds = earningUseCase.getPendingOrderIds(tableNumber);
        if (pendingIds.isEmpty()) {
            return new VBox(8);
        }

        VBox itemsList = new VBox(4);
        itemsList.setPadding(new Insets(8, 0, 0, 10));

        BigDecimal subtotale = BigDecimal.ZERO;
        for (Integer orderId : pendingIds) {
            subtotale = subtotale.add(appendOrderBlock(itemsList, orderId));
        }

        VBox section = new VBox(8);
        section.getStyleClass().add("order-section");
        section.getChildren().addAll(
                createPendingHeader(),
                new Separator(),
                itemsList,
                createSubtotalRow("Subtotale in preparazione: ", subtotale)
        );
        return section;
    }

    private HBox createPendingHeader() {
        Label lblTitle = new Label("⏳ In Preparazione");
        lblTitle.getStyleClass().addAll("order-header-title", "pending-status");

        HBox header = new HBox(10, lblTitle);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * Aggiunge al contenitore la riga dell'ordine e le righe dei singoli articoli.
     * @return il subtotale dell'ordine, utile per l'accumulo esterno.
     */
    private BigDecimal appendOrderBlock(VBox container, int orderId) {
        List<OrderItem> items = earningUseCase.getOrderItemsDetailed(orderId);
        if (items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        container.getChildren().add(createOrderHeaderRow(orderId));

        BigDecimal orderTotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal rowTotal = item.getPrezzoSnapshot()
                    .multiply(BigDecimal.valueOf(item.getQuantita()));
            orderTotal = orderTotal.add(rowTotal);
            container.getChildren().add(createItemRow(orderId, item, rowTotal));
        }
        return orderTotal;
    }

    private HBox createOrderHeaderRow(int orderId) {
        Label lblOrderSub = new Label("Ordine #" + orderId);
        lblOrderSub.getStyleClass().add(oinSTR);

        Button btnDelete = new Button("✖");
        btnDelete.getStyleClass().add("btn-delete-order");
        btnDelete.setOnAction(e -> {
            if (confirmDialog(
                    "Annullare l'ordine #" + orderId + "?",
                    "L'ordine in preparazione sarà marcato come cancellato.",
                    "Annulla ordine")) {
                earningUseCase.setOrderStatus(orderId, canceledSTR);
                refreshDataPreservingSelection();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, lblOrderSub, spacer, btnDelete);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createItemRow(int orderId, OrderItem item, BigDecimal rowTotal) {
        String nomeProdotto = item.getProduct() != null
                ? item.getProduct().getNome()
                : "???";

        Label lblName = new Label("⏳ " + item.getQuantita() + "x " + nomeProdotto);
        lblName.getStyleClass().add(oinSTR);

        Label lblPrice = new Label(formatCurrency(rowTotal));
        lblPrice.getStyleClass().add(oipSTR);

        Button btnRemove = new Button("✖");
        btnRemove.getStyleClass().add("btn-delete-item");
        btnRemove.setOnAction(e -> {
            if (confirmDialog(
                    "Eliminare questo articolo?",
                    item.getQuantita() + "x " + nomeProdotto
                            + " dall'ordine #" + orderId,
                    "Elimina")) {
                earningUseCase.cancelItemFromOrder(orderId, item.getId());
                refreshDataPreservingSelection();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, lblName, spacer, lblPrice, btnRemove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, 12));
        return row;
    }

    private HBox createSubtotalRow(String prefix, BigDecimal amount) {
        Label lbl = new Label(prefix + formatCurrency(amount));
        lbl.getStyleClass().add(oipSTR);

        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(6, 0, 0, 0));
        return row;
    }
    private boolean confirmDialog(String header, String body, String confirmLabel) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma");
        confirm.setHeaderText(header);
        confirm.setContentText(body);

        ButtonType ok = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(annullaSTR, ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(ok, cancel);

        return confirm.showAndWait().filter(ok::equals).isPresent();
    }
}