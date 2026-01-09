package com.example.rm.view;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
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
 * Controller per la vista di dettaglio dell'ordine.
 * Gestisce la visualizzazione completa delle informazioni di un ordine.
 */
public class OrderDetailController {

    private static final Logger logger = Logger.getLogger(OrderDetailController.class.getName());

    /* =======================
       COSTANTI
       ======================= */

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'alle' HH:mm");

    private static final String EMPTY_ITEMS_MESSAGE = "Nessun articolo trovato";
    private static final String BULLET_POINT = "•";

    // Emoji e simboli per stati
    private static final String EMOJI_PREPARING = "⏳";
    private static final String EMOJI_READY = "✓";
    private static final String EMOJI_CLOSED = "✓";
    private static final String EMOJI_UNKNOWN = "?";

    /* =======================
       FXML BINDINGS
       ======================= */

    @FXML private Label lblBack;
    @FXML private Label lblTitle;
    @FXML private VBox contentBox;

    /* =======================
       STATE
       ======================= */

    private Order order;

    /* =======================
       INITIALIZATION
       ======================= */

    @FXML
    public void initialize() {
        validateFXMLInjections();
    }

    /**
     * Valida che tutti i componenti FXML siano stati iniettati correttamente.
     */
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
     * Imposta l'ordine da visualizzare e aggiorna la vista.
     * @param order Ordine da visualizzare
     */
    public void setOrder(Order order) {
        if (order == null) {
            logger.log(Level.WARNING, "Tentativo di impostare ordine null");
            return;
        }

        this.order = order;
        render();
    }

    /* =======================
       RENDERING
       ======================= */

    /**
     * Renderizza i dettagli dell'ordine nella vista.
     */
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

    /**
     * Aggiorna il titolo con l'ID dell'ordine.
     */
    private void updateTitle() {
        if (lblTitle != null) {
            lblTitle.setText("Ordine #" + order.getId());
        }
    }

    /**
     * Pulisce il contenuto precedente.
     */
    private void clearContent() {
        contentBox.getChildren().clear();
    }

    /**
     * Renderizza tutte le sezioni dell'ordine.
     */
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

    /**
     * Crea la sezione con le informazioni generali dell'ordine.
     * @return VBox contenente le informazioni
     */
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

    /**
     * Aggiunge le informazioni sulla data e ora.
     */
    private void addDateInfo(VBox container) {
        Label label = createInfoLabel("Data e Ora:");
        Label value = createInfoValue(formatDateTime());
        container.getChildren().addAll(label, value);
    }

    /**
     * Aggiunge le informazioni sul tavolo.
     */
    private void addTableInfo(VBox container) {
        Label label = createInfoLabel("Tavolo:");
        Label value = createTableValue(String.valueOf(order.getTavolo()));
        container.getChildren().addAll(label, value);
    }

    /**
     * Aggiunge le informazioni sull'operatore.
     */
    private void addUserInfo(VBox container) {
        Label label = createInfoLabel("Gestito da:");
        Label value = createInfoValue(order.getUsername());
        container.getChildren().addAll(label, value);
    }

    /**
     * Aggiunge le note se presenti.
     */
    private void addNoteInfo(VBox container) {
        Label label = createNoteLabel("Note:");
        Label value = createNoteValue(order.getNote());
        container.getChildren().addAll(label, value);
    }

    /**
     * Crea la sezione con gli articoli ordinati.
     * @return VBox contenente gli articoli
     */
    private VBox createItemsSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 10, 0));

        Label header = createSectionHeader("Articoli Ordinati");
        section.getChildren().add(header);

        List<String> items = loadOrderItems();

        if (items.isEmpty()) {
            section.getChildren().add(createEmptyItemsLabel());
        } else {
            section.getChildren().add(createItemsList(items));
        }

        return section;
    }

    /**
     * Carica gli articoli dell'ordine dal database.
     * @return Lista di stringhe rappresentanti gli articoli
     */
    private List<String> loadOrderItems() {
        try {
            return DatabaseService.getOrderItemsForDisplay(order.getId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento articoli ordine #" + order.getId(), e);
            return List.of();
        }
    }

    /**
     * Crea la lista visuale degli articoli.
     * @param items Lista di articoli
     * @return VBox contenente la lista
     */
    private VBox createItemsList(List<String> items) {
        VBox itemsList = new VBox(8);
        itemsList.setPadding(new Insets(5, 0, 0, 10));

        for (String item : items) {
            HBox itemRow = createItemRow(item);
            itemsList.getChildren().add(itemRow);
        }

        return itemsList;
    }

    /**
     * Crea una singola riga articolo.
     * @param itemText Testo dell'articolo
     * @return HBox contenente la riga
     */
    private HBox createItemRow(String itemText) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label bullet = createBulletLabel();
        Label item = createItemLabel(itemText);

        row.getChildren().addAll(bullet, item);
        return row;
    }

    /**
     * Crea la sezione con i totali.
     * @return VBox contenente i totali
     */
    private VBox createTotalsSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(10, 0, 10, 0));
        section.getStyleClass().add("totals-section");

        HBox totalRow = createTotalRow();
        section.getChildren().add(totalRow);

        return section;
    }

    /**
     * Crea la riga con il totale.
     * @return HBox contenente il totale
     */
    private HBox createTotalRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label label = createTotalLabel("TOTALE:");
        Label value = createTotalValue(String.format("€%.2f", order.getTotale()));

        row.getChildren().addAll(label, value);
        return row;
    }

    /**
     * Crea la sezione con lo stato dell'ordine.
     * @return VBox contenente lo stato
     */
    private VBox createStatusSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 0, 0));

        Label header = createSectionHeader("Stato Ordine");
        HBox statusBadge = createStatusBadge();

        section.getChildren().addAll(header, statusBadge);
        return section;
    }

    /**
     * Crea il badge dello stato.
     * @return HBox contenente il badge
     */
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

    /**
     * Crea una label per il titolo di un'informazione.
     */
    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-label");
        return label;
    }

    /**
     * Crea una label per il valore di un'informazione.
     */
    private Label createInfoValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-value");
        return label;
    }

    /**
     * Crea una label per il valore del tavolo (evidenziato).
     */
    private Label createTableValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-value");
        return label;
    }

    /**
     * Crea una label per il titolo delle note.
     */
    private Label createNoteLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("note-label");
        return label;
    }

    /**
     * Crea una label per il valore delle note.
     */
    private Label createNoteValue(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("note-value");
        return label;
    }

    /**
     * Crea una label per l'header di sezione.
     */
    private Label createSectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-header");
        return label;
    }

    /**
     * Crea una label per lo stato vuoto degli articoli.
     */
    private Label createEmptyItemsLabel() {
        Label label = new Label(EMPTY_ITEMS_MESSAGE);
        label.getStyleClass().add("empty-items-label");
        return label;
    }

    /**
     * Crea una label per il bullet point.
     */
    private Label createBulletLabel() {
        Label label = new Label(BULLET_POINT);
        label.getStyleClass().add("bullet-label");
        return label;
    }

    /**
     * Crea una label per un articolo.
     */
    private Label createItemLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("item-label");
        return label;
    }

    /**
     * Crea una label per il titolo del totale.
     */
    private Label createTotalLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("total-label");
        return label;
    }

    /**
     * Crea una label per il valore del totale.
     */
    private Label createTotalValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("total-value");
        return label;
    }

    /* =======================
       FORMATTING HELPERS
       ======================= */

    /**
     * Formatta la data e ora dell'ordine.
     * @return Stringa formattata con iniziale maiuscola
     */
    private String formatDateTime() {
        String formatted = order.getDataOra().format(DATE_FORMATTER);
        return capitalizeFirst(formatted);
    }

    /**
     * Capitalizza il primo carattere di una stringa.
     * @param text Stringa da capitalizzare
     * @return Stringa con prima lettera maiuscola
     */
    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /* =======================
       STATUS MANAGEMENT
       ======================= */

    /**
     * Determina lo stato dell'ordine.
     * @return OrderStatus corrispondente
     */
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
            case "canceled":  // ✅ CORRETTO IL TYPO
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
     * Torna alla vista financial.
     */
    @FXML
    private void goBack() {
        try {
            logger.log(Level.INFO, "Ritorno a Financial View");

            View financialView = ViewFactory.forRole("financial");

            if (lblBack != null && lblBack.getScene() != null) {
                lblBack.getScene().setRoot(financialView.getRoot());
            } else {
                logger.log(Level.WARNING, "Scene non disponibile per la navigazione");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno a Financial", e);
        }
    }

    /* =======================
       INNER ENUM - Order Status
       ======================= */

    /**
     * Enum per la gestione degli stati dell'ordine.
     */
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