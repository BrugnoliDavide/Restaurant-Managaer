package com.example.rm.view;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller per la vista di dettaglio del prodotto.
 * Gestisce sia gli elementi FXML che la logica di business.
 */
public class ProductDetailController {

    private static final Logger logger = Logger.getLogger(ProductDetailController.class.getName());
    private static final double POSITIVE_TREND_THRESHOLD = 30.0;

    @FXML private Label lblBack;
    @FXML private Label lblName;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private VBox contentBox;



    private MenuProduct product;



    @FXML
    public void initialize() {
        setupEventHandlers();
    }


    private void setupEventHandlers() {
        lblBack.setOnMouseClicked(event -> onBack());
        btnEdit.setOnAction(event -> onEdit());
        btnDelete.setOnAction(event -> onDelete());
    }

    private void onBack() {
        try {
            View menu = ViewFactory.forRole("menu");
            contentBox.getScene().setRoot(menu.getRoot());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno al menu", e);
        }
    }

    private void onEdit() {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di modificare un prodotto null");
            return;
        }

        logger.log(Level.INFO, "Modifica prodotto: {0}", product.getNome());

        try {
            // Apri dialog di modifica con callback
            com.example.rm.view.component.AddProductDialog.displayEdit(product, success -> {
                if (success) {
                    logger.log(Level.INFO, "Prodotto modificato, ricarico dati");
                    reloadProduct();
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'apertura del dialog di modifica", e);
        }
    }

    private void onDelete() {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di eliminare un prodotto null");
            return;
        }

        logger.log(Level.INFO, "Eliminazione prodotto ID: {0}", product.getId());

        boolean success = DatabaseService.deleteProduct(product.getId());

        if (success) {
            logger.log(Level.INFO, "Prodotto eliminato con successo");
            onBack();
        } else {
            logger.log(Level.SEVERE, "Errore durante l'eliminazione del prodotto");
        }
    }

    /* =======================
       DATA MANAGEMENT
       ======================= */

    /**
     * Ricarica i dati del prodotto dal database.
     */
    private void reloadProduct() {
        if (product == null) {
            logger.log(Level.WARNING, "Impossibile ricaricare: product null");
            return;
        }

        try {
            // Ricarica il prodotto aggiornato dal database
            List<MenuProduct> allProducts = DatabaseService.getAllProducts();
            MenuProduct updatedProduct = allProducts.stream()
                    .filter(p -> p.getId() == product.getId())
                    .findFirst()
                    .orElse(null);

            if (updatedProduct != null) {
                this.product = updatedProduct;
                render();
            } else {
                logger.log(Level.WARNING, "Prodotto non trovato dopo il reload");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il reload del prodotto", e);
        }
    }

    /* =======================
       PUBLIC API
       ======================= */

    /**
     * Imposta il prodotto da visualizzare e aggiorna la vista.
     * @param product Il prodotto da visualizzare
     */
    public void setProduct(MenuProduct product) {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di impostare un prodotto null");
            return;
        }
        this.product = product;
        render();
    }

    /**
     * Restituisce il contenitore principale per i dettagli.
     * @return VBox contenitore
     */
    public VBox getContentBox() {
        return contentBox;
    }

    /* =======================
       RENDER LOGIC
       ======================= */

    /**
     * Renderizza i dettagli del prodotto nella vista.
     */
    private void render() {
        if (product == null) {
            logger.log(Level.WARNING, "Impossibile renderizzare: prodotto null");
            return;
        }

        // Aggiorna il nome del prodotto
        lblName.setText(product.getNome());

        // Pulisce il contenuto precedente
        contentBox.getChildren().clear();

        // Calcola le statistiche
        ProductStatistics stats = calculateStatistics();

        // Aggiunge le righe informative
        addDetailSection(stats);
    }

    /**
     * Aggiunge la sezione dei dettagli alla vista.
     * @param stats Le statistiche calcolate
     */
    private void addDetailSection(ProductStatistics stats) {
        addRow("Nome Prodotto", product.getNome());
        addRow("Categoria", product.getTipologia());
        addSeparator();

        addRow("Prezzo di Vendita", formatCurrency(product.getPrezzoVendita()));
        addRow("Costo di Realizzazione", formatCurrency(product.getCostoRealizzazione()));
        addRow("Margine Unitario", formatCurrency(calculateMargin()));
        addSeparator();

        addRow("Vendite (Ultimi 30 giorni)", String.valueOf(stats.salesLast30));
        addRow("Vendite (30 giorni precedenti)", String.valueOf(stats.salesPrevious30));
        addRow("Variazione Percentuale", formatPercentage(stats.variationPercent));
        addSeparator();

        addRow("Incasso Realizzato (30gg)", formatCurrency(stats.realizedIncome));

        if (product.getAllergeni() != null && !product.getAllergeni().isEmpty()) {
            addSeparator();
            addRow("Allergeni", product.getAllergeni());
        }
    }

    /**
     * Calcola le statistiche del prodotto.
     * @return Oggetto contenente le statistiche
     */
    private ProductStatistics calculateStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startLast30 = now.minusDays(30);
        LocalDateTime startPrev30 = now.minusDays(60);
        LocalDateTime endPrev30 = now.minusDays(30);

        long salesLast30 = DatabaseService.getQuantitySoldInDateRange(
                product.getId(),
                startLast30,
                now
        );

        long salesPrevious30 = DatabaseService.getQuantitySoldInDateRange(
                product.getId(),
                startPrev30,
                endPrev30
        );

        double realizedIncome = salesLast30 * product.getPrezzoVendita();

        double variationPercent = 0.0;
        if (salesPrevious30 > 0) {
            variationPercent = ((double) (salesLast30 - salesPrevious30) / salesPrevious30) * 100.0;
        }

        return new ProductStatistics(
                salesLast30,
                salesPrevious30,
                realizedIncome,
                variationPercent
        );
    }

    /* =======================
       UI HELPERS
       ======================= */

    /**
     * Aggiunge una riga di dettaglio alla vista.
     * @param title Titolo della riga
     * @param value Valore da visualizzare
     */
    private void addRow(String title, String value) {
        HBox row = new HBox();
        row.getStyleClass().add("product-detail-row");
        row.setPadding(new Insets(10, 0, 10, 0));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("row-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("row-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(titleLabel, spacer, valueLabel);
        contentBox.getChildren().add(row);
    }

    /**
     * Aggiunge un separatore alla vista.
     */
    private void addSeparator() {
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        contentBox.getChildren().add(separator);
    }

    /* =======================
       CALCULATION HELPERS
       ======================= */

    /**
     * Calcola il margine unitario del prodotto.
     * @return Margine unitario
     */
    private double calculateMargin() {
        return product.getPrezzoVendita() - product.getCostoRealizzazione();
    }

    /* =======================
       FORMATTING HELPERS
       ======================= */

    /**
     * Formatta un valore monetario.
     * @param amount Importo da formattare
     * @return Stringa formattata
     */
    private String formatCurrency(double amount) {
        return String.format("€ %.2f", amount);
    }

    /**
     * Formatta una percentuale.
     * @param percent Percentuale da formattare
     * @return Stringa formattata
     */
    private String formatPercentage(double percent) {
        String formatted = String.format("%.1f%%", percent);
        if (percent >= POSITIVE_TREND_THRESHOLD) {
            return formatted + " ↑";
        } else if (percent < 0) {
            return formatted + " ↓";
        }
        return formatted;
    }

    /* =======================
       INNER CLASS - Statistics
       ======================= */

    /**
     * Classe di supporto per contenere le statistiche del prodotto.
     */
    private static class ProductStatistics {
        final long salesLast30;
        final long salesPrevious30;
        final double realizedIncome;
        final double variationPercent;

        ProductStatistics(long salesLast30, long salesPrevious30,
                          double realizedIncome, double variationPercent) {
            this.salesLast30 = salesLast30;
            this.salesPrevious30 = salesPrevious30;
            this.realizedIncome = realizedIncome;
            this.variationPercent = variationPercent;
        }
    }
}