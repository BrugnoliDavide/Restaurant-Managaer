package com.example.rm.view;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.LoggerService;
import com.example.rm.view.component.AddProductDialog;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDetailController {

    @FXML private Label lblBack;
    @FXML private Label lblName;
    @FXML private VBox contentBox;

    private static final Logger logger = LoggerService.getLogger(ProductDetailController.class);

    private MenuProduct product;

    public void setProduct(MenuProduct product) {
        this.product = product;
        render();
    }

    private void render() {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di render con prodotto null");
            return;
        }

        // Imposta il nome nell'header
        lblName.setText(product.getNome());

        // Pulisce il contentBox da eventuali contenuti precedenti
        contentBox.getChildren().clear();

        // Popola il contentBox con i dettagli del prodotto
        addDetailRow("Tipologia", product.getTipologia());
        addSeparator();

        addDetailRow("Prezzo di Vendita", formatPrice(product.getPrezzoVendita()));
        addDetailRow("Costo di Realizzazione", formatPrice(product.getCostoRealizzazione()));
        addDetailRow("Margine di Profitto", formatPrice(calculateMargin()));
        addSeparator();

        String allergeni = product.getAllergeni();
        addDetailRow("Allergeni", allergeni != null && !allergeni.trim().isEmpty()
                ? allergeni
                : "Nessuno");
        addSeparator();

        // Mostra statistiche di vendita
        long quantitaVenduta = DatabaseService.getQuantitySold(product.getNome());
        addDetailRow("Quantità Venduta", String.valueOf(quantitaVenduta));

        if (quantitaVenduta > 0) {
            double ricavoTotale = quantitaVenduta * product.getPrezzoVendita();
            double costoTotale = quantitaVenduta * product.getCostoRealizzazione();
            double profittoTotale = ricavoTotale - costoTotale;

            addDetailRow("Ricavo Totale", formatPrice(ricavoTotale));
            addDetailRow("Profitto Totale", formatPrice(profittoTotale));
        }
    }

    /**
     * Aggiunge una riga di dettaglio al contentBox
     */
    private void addDetailRow(String label, String value) {
        HBox row = new HBox(15);
        row.setPadding(new Insets(8, 0, 8, 0));

        Label lblLabel = new Label(label + ":");
        lblLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 180px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #333;");
        lblValue.setWrapText(true);

        row.getChildren().addAll(lblLabel, lblValue);
        contentBox.getChildren().add(row);
    }

    /**
     * Aggiunge un separatore visivo
     */
    private void addSeparator() {
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        contentBox.getChildren().add(separator);
    }

    /**
     * Formatta un prezzo in formato valuta
     */
    private String formatPrice(double price) {
        return String.format("€ %.2f", price);
    }

    /**
     * Calcola il margine di profitto per unità
     */
    private double calculateMargin() {
        return product.getPrezzoVendita() - product.getCostoRealizzazione();
    }

    @FXML
    private void initialize() {
        setupBackHover();
    }

    @FXML
    private void goBack() {
        logger.log(Level.INFO, "Invocazione GoBack da dettaglio prodotto");
        View menuView = ViewFactory.forRole("menu");
        lblBack.getScene().setRoot(menuView.getRoot());
    }

    @FXML
    private void onDelete() {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di eliminazione con prodotto null");
            return;
        }

        boolean success = DatabaseService.deleteProduct(product.getId());

        if (success) {
            logger.log(Level.INFO, "Prodotto {0} eliminato con successo", product.getNome());
            goBack();
        } else {
            logger.log(Level.SEVERE, "Impossibile eliminare il prodotto: {0}", product.getNome());
            // TODO: Mostrare un Alert all'utente
        }
    }

    @FXML
    private void onEdit() {
        if (product == null) {
            logger.log(Level.WARNING, "Tentativo di modifica con prodotto null");
            return;
        }

        logger.log(Level.INFO, "Apertura dialog di modifica per prodotto: {0}", product.getNome());

        // Apre il dialog di modifica con callback
        AddProductDialog.displayEdit(product, success -> {
            if (success) {
                logger.log(Level.INFO, "Modifica completata con successo");
                // Ricarica i dati del prodotto aggiornato e rimani sulla pagina
                refreshProductData();
            } else {
                logger.log(Level.INFO, "Modifica annullata dall'utente");
                // Resta sulla pagina di dettaglio senza modifiche
            }
        });
    }

    /**
     * Ricarica i dati del prodotto dal database per riflettere le modifiche
     */
    private void refreshProductData() {
        if (product == null || product.getId() <= 0) {
            logger.log(Level.WARNING, "Impossibile ricaricare: prodotto non valido");
            return;
        }

        // Ricarica il prodotto aggiornato dal database
        MenuProduct updatedProduct = DatabaseService.getProductById(product.getId());

        if (updatedProduct != null) {
            this.product = updatedProduct;
            render(); // Ri-renderizza la vista con i nuovi dati
            logger.log(Level.INFO, "Dati prodotto ricaricati con successo");
        } else {
            logger.log(Level.WARNING, "Prodotto non trovato nel database dopo l'aggiornamento");
            // Se il prodotto è stato eliminato, torna al menu
            goBack();
        }
    }

    /**
     * Configura gli effetti hover per il link "Torna indietro"
     */
    private void setupBackHover() {
        String normal = "-fx-text-fill: #888; -fx-cursor: hand;";
        String hover = "-fx-text-fill: #333; -fx-underline: true; -fx-cursor: hand;";

        lblBack.setStyle(normal);
        lblBack.setOnMouseEntered(e -> lblBack.setStyle(hover));
        lblBack.setOnMouseExited(e -> lblBack.setStyle(normal));
    }
}