package com.example.rm.view.component;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Controller per OrderRowItem.fxml
 * Utilizzato in EarningController per la gestione degli ordini da pagare.
 * Supporta la selezione visiva degli ordini.
 *
 * @author Restaurant Management System
 * @version 1.0
 */
public class OrderRowItemController {

    // ========================================
    // FXML BINDINGS
    // ========================================

    @FXML private HBox root;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;

    // ========================================
    // STATE
    // ========================================

    private Order order;
    private Consumer<Order> onSelectCallback;
    private boolean isSelected = false;

    private static final Logger logger =
            Logger.getLogger(OrderRowItemController.class.getName());

    // ========================================
    // STILI CSS
    // ========================================

    private static final String STYLE_NORMAL =
            "-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; " +
                    "-fx-background-color: white; -fx-cursor: hand; -fx-padding: 12;";

    private static final String STYLE_HOVER =
            "-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; " +
                    "-fx-background-color: #F9F9F9; -fx-cursor: hand; -fx-padding: 12;";

    private static final String STYLE_SELECTED =
            "-fx-border-color: #2ecc71; -fx-border-width: 0 0 3 0; " +
                    "-fx-background-color: #E8F8F5; -fx-cursor: hand; -fx-padding: 12;";

    // ========================================
    // PUBLIC METHODS
    // ========================================

    /**
     * Imposta l'ordine da visualizzare nella riga.
     *
     * @param order L'ordine da visualizzare (non può essere null)
     * @param onSelectCallback Callback invocato quando l'ordine viene selezionato (può essere null)
     */
    public void setOrder(Order order, Consumer<Order> onSelectCallback) {
        if (order == null) {
            logger.warning("Tentativo di impostare un ordine NULL");
            return;
        }

        this.order = order;
        this.onSelectCallback = onSelectCallback;

        updateUI();
        updateStyle();
    }

    /**
     * Imposta lo stato di selezione dell'ordine.
     * Quando selezionato, la riga viene evidenziata con bordo verde.
     *
     * @param selected true se l'ordine è selezionato, false altrimenti
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateStyle();
    }

    /**
     * Restituisce l'ordine associato a questa riga.
     *
     * @return l'ordine corrente
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Verifica se l'ordine è attualmente selezionato.
     *
     * @return true se selezionato, false altrimenti
     */
    public boolean isSelected() {
        return isSelected;
    }

    // ========================================
    // PRIVATE METHODS
    // ========================================

    /**
     * Aggiorna l'interfaccia utente con i dati dell'ordine.
     * Popola i label lblTitle e lblSubtitle.
     */
    private void updateUI() {
        updateTitle();
        updateSubtitle();
    }

    /**
     * Aggiorna il titolo con informazioni principali dell'ordine.
     * Formato: "Ordine #123 - Tavolo 5 - €45.50"
     */
    private void updateTitle() {
        if (lblTitle != null) {
            String title = String.format("Ordine #%d - Tavolo %d - €%.2f",
                    order.getId(),
                    order.getTavolo(),
                    order.getTotale());
            lblTitle.setText(title);
        } else {
            logger.warning("lblTitle è null - verificare fx:id in OrderRowItem.fxml");
        }
    }

    /**
     * Aggiorna il sottotitolo con orario, operatore e articoli.
     * Formato: "18:30 - Manager - 2x Pizza, 1x Pasta"
     */
    private void updateSubtitle() {
        if (lblSubtitle != null) {
            StringBuilder subtitle = new StringBuilder();

            // Aggiungi orario (formato HH:mm)
            if (order.getDataOra() != null) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                subtitle.append(order.getDataOra().format(timeFormatter));
            }

            // Aggiungi username/operatore
            if (order.getUsername() != null && !order.getUsername().isEmpty()) {
                subtitle.append(" - ").append(order.getUsername());
            }

            // Aggiungi articoli dell'ordine
            List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());
            if (!items.isEmpty()) {
                subtitle.append(" - ");

                // Limita a massimo 3 articoli per non sovraccaricare la vista
                int maxItems = Math.min(3, items.size());
                subtitle.append(String.join(", ", items.subList(0, maxItems)));

                // Aggiungi "..." se ci sono più articoli
                if (items.size() > 3) {
                    subtitle.append("...");
                }
            }

            lblSubtitle.setText(subtitle.toString());
        } else {
            logger.warning("lblSubtitle è null - verificare fx:id in OrderRowItem.fxml");
        }
    }

    /**
     * Aggiorna lo stile visivo della riga in base allo stato di selezione.
     */
    private void updateStyle() {
        if (root == null) {
            logger.warning("root è null - impossibile aggiornare lo stile");
            return;
        }

        if (isSelected) {
            root.setStyle(STYLE_SELECTED);
        } else {
            root.setStyle(STYLE_NORMAL);
        }
    }

    // ========================================
    // EVENT HANDLERS (chiamati da FXML)
    // ========================================

    /**
     * Gestisce l'evento MouseEntered (hover).
     * Cambia lo sfondo solo se l'ordine non è selezionato.
     */
    @FXML
    private void onHover() {
        if (root != null && !isSelected) {
            root.setStyle(STYLE_HOVER);
        }
    }

    /**
     * Gestisce l'evento MouseExited (fine hover).
     * Ripristina lo stile normale o selezionato.
     */
    @FXML
    private void onExit() {
        if (root != null) {
            updateStyle(); // Ritorna allo stato normale o selezionato
        }
    }

    /**
     * Gestisce l'evento MouseClicked (click sulla riga).
     * Invoca il callback di selezione se presente.
     */
    @FXML
    private void onClick() {
        if (order == null) {
            logger.warning("onClick invocato ma order è null");
            return;
        }

        logger.info("Click su ordine #" + order.getId());

        if (onSelectCallback != null) {
            onSelectCallback.accept(order);
        }
    }
}