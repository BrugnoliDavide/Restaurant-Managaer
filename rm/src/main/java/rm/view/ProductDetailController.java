package rm.view;

import rm.controller.MenuService;
import rm.controller.MenuUseCase;
import rm.dao.DatabaseProductDAO;
import rm.model.MenuProduct;
import rm.service.OrderService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import rm.view.component.AddProductDialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller per la vista di dettaglio del prodotto.
 *
 * <p>Usa {@link MenuUseCase} per tutte le operazioni sui prodotti,
 * rispettando la separazione dei layer: View → Controller → Service → DAO.</p>
 *
 * <p>L'unica eccezione è {@code delete}, che delega direttamente al DAO
 * tramite il metodo {@link MenuUseCase} apposito.</p>
 */
public class ProductDetailController {

    private static final Logger logger =
            Logger.getLogger(ProductDetailController.class.getName());
    private static final double POSITIVE_TREND_THRESHOLD = 30.0;

    @FXML private Label  lblBack;
    @FXML private Label  lblName;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private VBox   contentBox;

    private MenuProduct product;

    /**
     * Accesso ai prodotti tramite il layer Service.
     * DatabaseProductDAO non viene più referenziato direttamente da questo controller.
     */
    private final MenuUseCase menuUseCase = new MenuService();

    // =========================================================================
    //  Inizializzazione FXML
    // =========================================================================

    @FXML
    public void initialize() {
        lblBack.setOnMouseClicked(event -> onBack());
        btnEdit.setOnAction(event -> onEdit());
        btnDelete.setOnAction(event -> onDelete());
    }

    // =========================================================================
    //  Navigazione
    // =========================================================================

    private void onBack() {
        try {
            View menu = ViewFactory.forRole("menu");
            contentBox.getScene().setRoot(menu.getRoot());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno al menu", e);
        }
    }

    // =========================================================================
    //  Azioni
    // =========================================================================

    private void onEdit() {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di modificare un prodotto null");
            return;
        }
        logger.log(Level.INFO, "Modifica prodotto: {0}", product.getNome());
        try {
            AddProductDialog.displayEdit(product, success -> {
                if (Boolean.TRUE.equals(success)) {
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

        // delete non ha un corrispettivo in MenuUseCase → usiamo il DAO tramite
        // il cast all'implementazione concreta, oppure aggiungiamo il metodo
        // all'interfaccia. Per ora usiamo il DAO direttamente come prima,
        // ma isoliamo la dipendenza in un solo punto.
        boolean success = new DatabaseProductDAO().delete((long) product.getId());

        if (success) {
            logger.log(Level.INFO, "Prodotto eliminato con successo");
            onBack();
        } else {
            logger.log(Level.SEVERE, "Errore durante l'eliminazione del prodotto");
        }
    }

    // =========================================================================
    //  Reload
    // =========================================================================

    /**
     * Ricarica il prodotto aggiornato usando il Service (che gestisce la
     * conversione Bean → Model tramite BeanMapper internamente).
     */
    private void reloadProduct() {
        if (product == null) {
            logger.log(Level.WARNING, "Impossibile ricaricare: product null");
            return;
        }
        try {
            // menuUseCase.getProductById restituisce già MenuProduct (via BeanMapper nel Service)
            MenuProduct updated = menuUseCase.getProductById(product.getId());
            if (updated != null) {
                this.product = updated;
                render();
            } else {
                logger.log(Level.WARNING, "Prodotto non trovato dopo il reload");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il reload del prodotto", e);
        }
    }

    // =========================================================================
    //  API pubblica
    // =========================================================================

    /**
     * Imposta il prodotto da visualizzare e aggiorna la vista.
     */
    public void setProduct(MenuProduct product) {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di impostare un prodotto null");
            return;
        }
        this.product = product;
        render();
    }

    // =========================================================================
    //  Rendering
    // =========================================================================

    private void render() {
        if (product == null) {
            logger.log(Level.WARNING, "Impossibile renderizzare: prodotto null");
            return;
        }
        lblName.setText(product.getNome());
        contentBox.getChildren().clear();
        addDetailSection(calculateStatistics());
    }

    private void addDetailSection(ProductStatistics stats) {
        addRow("Nome Prodotto",  product.getNome());
        addRow("Categoria",      product.getTipologia());
        addSeparator();

        addRow("Prezzo di Vendita",      formatCurrency(product.getPrezzoVendita()));
        addRow("Costo di Realizzazione", formatCurrency(product.getCostoRealizzazione()));
        addRow("Margine Unitario",       formatCurrency(calculateMargin()));
        addSeparator();

        addRow("Vendite (Ultimi 30 giorni)",      String.valueOf(stats.salesLast30));
        addRow("Vendite (30 giorni precedenti)",  String.valueOf(stats.salesPrevious30));
        addRow("Variazione Percentuale",          formatPercentage(stats.variationPercent));
        addSeparator();

        addRow("Incasso Realizzato (30gg)", formatCurrency(stats.realizedIncome));

        if (product.getAllergeni() != null && !product.getAllergeni().isEmpty()) {
            addSeparator();
            addRow("Allergeni", product.getAllergeni());
        }
    }

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

    private void addSeparator() {
        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));
        contentBox.getChildren().add(sep);
    }

    // =========================================================================
    //  Calcoli
    // =========================================================================

    private ProductStatistics calculateStatistics() {
        LocalDateTime now          = LocalDateTime.now(Clock.systemDefaultZone());
        LocalDateTime startLast30  = now.minusDays(30);
        LocalDateTime startPrev30  = now.minusDays(60);
        LocalDateTime endPrev30    = now.minusDays(30);

        long salesLast30    = OrderService.getQuantitySoldInRange(product.getId(), startLast30, now);
        long salesPrevious30 = OrderService.getQuantitySoldInRange(product.getId(), startPrev30, endPrev30);

        BigDecimal realizedIncome = product.getPrezzoVendita()
                .multiply(BigDecimal.valueOf(salesLast30));

        double variationPercent = 0.0;
        if (salesPrevious30 > 0) {
            variationPercent = ((double)(salesLast30 - salesPrevious30) / salesPrevious30) * 100.0;
        }

        return new ProductStatistics(salesLast30, salesPrevious30, realizedIncome, variationPercent);
    }

    private BigDecimal calculateMargin() {
        return product.getPrezzoVendita().subtract(product.getCostoRealizzazione());
    }

    private String formatCurrency(BigDecimal amount) {
        return "€ " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercentage(double percent) {
        String formatted = String.format("%.1f%%", percent);
        if (percent >= POSITIVE_TREND_THRESHOLD) return formatted + " ↑";
        if (percent < 0)                         return formatted + " ↓";
        return formatted;
    }

    // =========================================================================
    //  Inner class statistica
    // =========================================================================

    private static class ProductStatistics {
        final long       salesLast30;
        final long       salesPrevious30;
        final BigDecimal realizedIncome;
        final double     variationPercent;

        ProductStatistics(long salesLast30, long salesPrevious30,
                          BigDecimal realizedIncome, double variationPercent) {
            this.salesLast30     = salesLast30;
            this.salesPrevious30 = salesPrevious30;
            this.realizedIncome  = realizedIncome;
            this.variationPercent = variationPercent;
        }
    }
}