package com.example.rm.view;

import com.example.rm.app.SceneManager;
import com.example.rm.controller.OrderUseCase;
import com.example.rm.controller.OrderService;
import com.example.rm.model.Order;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller refactorizzato per la vista di dettaglio dell'ordine.
 * USA OrderUseCase esistente invece di accedere direttamente a DatabaseService.
 */
public class OrderDetailController {

    private static final Logger logger = Logger.getLogger(OrderDetailController.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'alle' HH:mm");

    private static final String EMPTY_ITEMS_MESSAGE = "Nessun articolo trovato";
    private static final String BULLET_POINT = "•";

    private static final String EMOJI_PREPARING = "⏳";
    private static final String EMOJI_READY = "✓";
    private static final String EMOJI_CLOSED = "✓";
    private static final String EMOJI_UNKNOWN = "?";

    @FXML private Label lblBack;
    @FXML private Label lblTitle;
    @FXML private VBox contentBox;


    private OrderUseCase orderUseCase;
    private SceneManager sceneManager;

    private Order order;

    /**
     * Costruttore di default.
     * Inizializza con OrderService di produzione.
     */
    public OrderDetailController() {
        this.orderUseCase = new OrderService();
    }

    /**
     * Imposta il UseCase (dependency injection per test).
     */
    public void setOrderUseCase(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    /**
     * Imposta lo SceneManager per la navigazione.
     */
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    public void initialize() {
        validateFXMLInjections();
    }

    private void validateFXMLInjections() {
        if (lblBack == null) {
            logger.log(Level.SEVERE, "lblBack non iniettato da FXML");
        }
        if (lblTitle == null) {
            logger.log(Level.SEVERE, "lblTitle non iniettato da FXML");
        }
        if (contentBox == null) {
            logger.log(Level.SEVERE, "contentBox non iniettato da FXML");
        }
    }

    /* =======================
       PUBLIC API
       ======================= */

    /**
     * Carica un ordine tramite ID usando OrderUseCase.
     */
    public void loadOrder(int orderId) {
        if (orderId <= 0) {
            logger.log(Level.WARNING, "ID ordine non valido: {0}", orderId);
            showError("ID ordine non valido");
            return;
        }

        // ✅ USA IL METODO DELL'INTERFACCIA ESISTENTE
        Order loadedOrder = orderUseCase.getOrderById(orderId);

        if (loadedOrder == null) {
            logger.log(Level.WARNING, "Ordine non trovato con ID: {0}", orderId);
            showError("Ordine non trovato");
            return;
        }

        this.order = loadedOrder;
        render();
    }

    /**
     * Imposta l'ordine direttamente (quando già caricato).
     */
    public void setOrder(Order order) {
        if (order == null) {
            logger.log(Level.WARNING, "Tentativo di impostare ordine null");
            showError("Ordine non valido");
            return;
        }

        this.order = order;
        render();
    }

    /* =======================
       RENDERING
       ======================= */

    private void render() {
        if (order == null) {
            logger.log(Level.WARNING, "Impossibile renderizzare: ordine null");
            return;
        }

        if (contentBox == null) {
            logger.log(Level.SEVERE, "Impossibile renderizzare: contentBox null");
            return;
        }

        updateTitle();
        clearContent();
        renderSections();
    }

    private void updateTitle() {
        if (lblTitle != null) {
            lblTitle.setText("Ordine #" + order.getId());
        }
    }

    private void clearContent() {
        contentBox.getChildren().clear();
    }

    private void renderSections() {
        VBox infoSection = createInfoSection();
        VBox itemsSection = createItemsSection();
        VBox totalsSection = createTotalsSection();
        VBox statusSection = createStatusSection();

        contentBox.getChildren().addAll(
                infoSection,
                new Separator(),
                itemsSection,
                new Separator(),
                totalsSection,
                new Separator(),
                statusSection
        );
    }

    /* =======================
       SECTION CREATORS
       ======================= */

    private VBox createInfoSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 0, 10, 0));

        addDateInfo(section);
        addTableInfo(section);
        addUserInfo(section);

        if (order.hasNote()) {
            addNoteInfo(section);
        }

        return section;
    }

    private void addDateInfo(VBox container) {
        Label label = createInfoLabel("Data e Ora:");
        Label value = createInfoValue(formatDateTime());
        container.getChildren().addAll(label, value);
    }

    private void addTableInfo(VBox container) {
        Label label = createInfoLabel("Tavolo:");
        Label value = createTableValue(String.valueOf(order.getTavolo()));
        container.getChildren().addAll(label, value);
    }

    private void addUserInfo(VBox container) {
        Label label = createInfoLabel("Gestito da:");
        Label value = createInfoValue(order.getUsername());
        container.getChildren().addAll(label, value);
    }

    private void addNoteInfo(VBox container) {
        Label label = createNoteLabel("Note:");
        Label value = createNoteValue(order.getNote());
        container.getChildren().addAll(label, value);
    }

    /**
     * Crea la sezione articoli usando OrderUseCase.
     */
    private VBox createItemsSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 10, 0));

        Label header = createSectionHeader("Articoli Ordinati");
        section.getChildren().add(header);

        // ✅ USA IL METODO loadOrderItemsForDisplay DELL'INTERFACCIA ESISTENTE
        List<String> items = loadOrderItems();

        if (items.isEmpty()) {
            section.getChildren().add(createEmptyItemsLabel());
        } else {
            section.getChildren().add(createItemsList(items));
        }

        return section;
    }

    /**
     * Carica articoli usando il metodo esistente di OrderUseCase.
     */
    private List<String> loadOrderItems() {
        try {
            // ✅ USA loadOrderItemsForDisplay CHE GIÀ ESISTE
            return orderUseCase.loadOrderItemsForDisplay(order.getId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento articoli ordine #" + order.getId(), e);
            return List.of();
        }
    }

    private VBox createItemsList(List<String> items) {
        VBox itemsList = new VBox(8);
        itemsList.setPadding(new Insets(5, 0, 0, 10));

        for (String item : items) {
            HBox itemRow = createItemRow(item);
            itemsList.getChildren().add(itemRow);
        }

        return itemsList;
    }

    private HBox createItemRow(String itemText) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label bullet = createBulletLabel();
        Label item = createItemLabel(itemText);

        row.getChildren().addAll(bullet, item);
        return row;
    }

    private VBox createTotalsSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(10, 0, 10, 0));
        section.getStyleClass().add("totals-section");

        HBox totalRow = createTotalRow();
        section.getChildren().add(totalRow);

        return section;
    }

    private HBox createTotalRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label label = createTotalLabel("TOTALE:");
        Label value = createTotalValue(String.format("€%.2f", order.getTotale()));

        row.getChildren().addAll(label, value);
        return row;
    }

    private VBox createStatusSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 0, 0));

        Label header = createSectionHeader("Stato Ordine");
        HBox statusBadge = createStatusBadge();

        section.getChildren().addAll(header, statusBadge);
        return section;
    }

    private HBox createStatusBadge() {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(10));
        badge.setMaxWidth(200);

        OrderStatus status = determineOrderStatus();
        badge.getStyleClass().add(status.getStyleClass());

        Label statusLabel = new Label(status.getDisplayText());
        statusLabel.getStyleClass().add("status-label");

        badge.getChildren().add(statusLabel);
        return badge;
    }

    /* =======================
       LABEL CREATORS
       ======================= */

    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-label");
        return label;
    }

    private Label createInfoValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-value");
        return label;
    }

    private Label createTableValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-value");
        return label;
    }

    private Label createNoteLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("note-label");
        return label;
    }

    private Label createNoteValue(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("note-value");
        return label;
    }

    private Label createSectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-header");
        return label;
    }

    private Label createEmptyItemsLabel() {
        Label label = new Label(EMPTY_ITEMS_MESSAGE);
        label.getStyleClass().add("empty-items-label");
        return label;
    }

    private Label createBulletLabel() {
        Label label = new Label(BULLET_POINT);
        label.getStyleClass().add("bullet-label");
        return label;
    }

    private Label createItemLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("item-label");
        return label;
    }

    private Label createTotalLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("total-label");
        return label;
    }

    private Label createTotalValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("total-value");
        return label;
    }

    /* =======================
       FORMATTING HELPERS
       ======================= */

    private String formatDateTime() {
        String formatted = order.getDataOra().format(DATE_FORMATTER);
        return capitalizeFirst(formatted);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /* =======================
       STATUS MANAGEMENT
       ======================= */

    private OrderStatus determineOrderStatus() {
        String status = order.getStatus();

        if (status == null) {
            return OrderStatus.UNKNOWN;
        }

        switch (status.toLowerCase()) {
            case "to-do":
                return OrderStatus.PREPARING;
            case "ready":
                return OrderStatus.READY;
            case "closed":
                return OrderStatus.CLOSED;
            case "delivered":
                return OrderStatus.DELIVERED;
            case "canceled":
                return OrderStatus.CANCELED;
            default:
                logger.log(Level.WARNING, "Stato ordine sconosciuto: {0}", status);
                return OrderStatus.UNKNOWN;
        }
    }

    /* =======================
       NAVIGATION
       ======================= */

    /**
     * Torna alla vista financial usando SceneManager.
     */
    @FXML
    private void goBack() {
        try {
            logger.log(Level.INFO, "Ritorno a Financial View");
            SceneManager.showFinancial();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno a Financial", e);
            showError("Errore durante la navigazione");
        }
    }




    private void showError(String message) {
        if (contentBox != null) {
            contentBox.getChildren().clear();

            Label errorLabel = new Label(message);
            errorLabel.getStyleClass().add("error-label");
            errorLabel.setWrapText(true);

            contentBox.getChildren().add(errorLabel);
        }
    }

    private enum OrderStatus {
        PREPARING("status-preparing", EMOJI_PREPARING + " In Preparazione"),
        READY("status-ready", EMOJI_READY + " Pronto"),
        CLOSED("status-closed", EMOJI_CLOSED + " Pagato e Chiuso"),
        DELIVERED("status-delivered", "Da Pagare"),
        CANCELED("status-canceled", "Cancellato"),
        UNKNOWN("status-unknown", EMOJI_UNKNOWN + " Sconosciuto");

        private final String styleClass;
        private final String displayText;

        OrderStatus(String styleClass, String displayText) {
            this.styleClass = styleClass;
            this.displayText = displayText;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public String getDisplayText() {
            return displayText;
        }
    }
}