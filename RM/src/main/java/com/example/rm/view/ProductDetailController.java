package com.example.rm.view;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.LoggerService;
import com.example.rm.view.component.AddProductDialog;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath; // IMPORTANTE: Usiamo SVGPath ora

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class ProductDetailController {

    @FXML private Label lblBack;
    @FXML private Label lblName;
    @FXML private VBox contentBox;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private static final Logger logger = LoggerService.getLogger(ProductDetailController.class);
    private static final double TREND_PERCENTAGE_THRESHOLD = 5.0;

    private MenuProduct product;

    public void setProduct(MenuProduct product) {
        this.product = product;
        render();
    }

    private void render() {
        if (product == null) return;
        // Il nome ora è settato nell'FXML con stile grande, qui mettiamo solo il testo
        lblName.setText(product.getNome());
        contentBox.getChildren().clear();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        long salesCurrentMonth = DatabaseService.getQuantitySoldInDateRange(product.getId(), thirtyDaysAgo, now);
        long salesPreviousMonth = DatabaseService.getQuantitySoldInDateRange(product.getId(), sixtyDaysAgo, thirtyDaysAgo);
        double realizedIncome = salesCurrentMonth * (product.getPrezzoVendita() - product.getCostoRealizzazione());

        addSimpleRow("Category", product.getTipologia());
        addSeparator();
        addSimpleRow("Price", formatPrice(product.getPrezzoVendita()));
        addSeparator();

        // Sold Quantity (Con Trendline SVG)
        addComplexRowWithTrend(
                "Sold quantity",
                "quantities sold\nin the last 30 days",
                String.valueOf(salesCurrentMonth),
                salesCurrentMonth,
                salesPreviousMonth
        );
        addSeparator();

        addSimpleRow("Realization price", formatPrice(product.getCostoRealizzazione()));
        addSeparator();

        addComplexRow(
                "Realized income",
                "The product of sold quantity and the\ndifference between Price and Realization price",
                formatPrice(realizedIncome)
        );

        if (product.getAllergeni() != null && !product.getAllergeni().isEmpty()) {
            addSeparator();
            addComplexRow("Allergeni", product.getAllergeni(), "");
        }
    }

    // --- HELPER METODI UI ---

    private void addSimpleRow(String title, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0)); // Padding leggermente aumentato

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #555;"); // Colore più morbido

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 16px; -fx-text-fill: #222; -fx-font-weight: 500;");

        row.getChildren().addAll(lblTitle, spacer, lblValue);
        contentBox.getChildren().add(row);
    }

    private void addComplexRow(String title, String subtitle, String value) {
        addComplexRowWithTrend(title, subtitle, value, 0, 0);
    }

    private void addComplexRowWithTrend(String title, String subtitle, String value, long current, long previous) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(18, 0, 18, 0)); // Padding aumentato per le righe complesse

        // --- COLONNA SINISTRA ---
        VBox texts = new VBox(6);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #222; -fx-font-weight: bold;");

        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #888; -fx-line-spacing: 2px;");
        lblSub.setWrapText(true);

        texts.getChildren().addAll(lblTitle, lblSub);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- COLONNA DESTRA (Valore + Icona SVG) ---
        HBox rightSide = new HBox(12);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        if (current > 0 || previous > 0) {
            if (value.equals(String.valueOf(current))) {
                // Usiamo Node generico perché SVGPath è un Node
                Node trendIcon = calculateTrendIconSVG(current, previous);
                if (trendIcon != null) {
                    rightSide.getChildren().add(trendIcon);
                }
            }
        }

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 22px; -fx-text-fill: #222; -fx-font-weight: bold;");

        rightSide.getChildren().add(lblValue);
        row.getChildren().addAll(texts, spacer, rightSide);
        contentBox.getChildren().add(row);
    }

    private Node calculateTrendIconSVG(long current, long previous) {
        if (previous == 0 && current == 0) return null;

        double percentage;
        if (previous == 0) percentage = 100.0; // Crescita infinita
        else percentage = ((double)(current - previous) / previous) * 100.0;

        if (percentage >= TREND_PERCENTAGE_THRESHOLD) {
            // Icona Grafico in crescita (Modern Chart Up)
            return createSVGIcon(
                    "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z",
                    "#2ecc71" // Verde
            );
        } else if (percentage <= -TREND_PERCENTAGE_THRESHOLD) {
            // Icona Freccia giù arrotondata (Rounded Down Arrow)
            return createSVGIcon(
                    "M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z",
                    "#e74c3c" // Rosso
            );
        }
        return null;
    }

    /**
     * Helper per creare un oggetto SVGPath da una stringa di percorso
     */
    private SVGPath createSVGIcon(String pathContent, String colorHex) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathContent);
        svg.setFill(Color.web(colorHex));
        // Scala leggermente per adattarsi bene accanto al testo
        svg.setScaleX(1.1);
        svg.setScaleY(1.1);
        return svg;
    }

    private void addSeparator() {
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #EEEEEE;");
        contentBox.getChildren().add(separator);
    }

    private String formatPrice(double price) {
        return String.format("%.2f€", price);
    }


    @FXML private void initialize() { setupBackHover();
        lblBack.setOnMouseClicked(event -> goBack());
    }

    @FXML private void goBack() {
        View menuView = ViewFactory.forRole("menu");
        lblBack.getScene().setRoot(menuView.getRoot());
    }
    @FXML private void onDelete() {
        if(DatabaseService.deleteProduct(product.getId())) goBack();
    }
    @FXML private void onEdit() {
        AddProductDialog.displayEdit(product, success -> { if(success) refreshProductData(); });
    }
    private void refreshProductData() {
        MenuProduct updated = DatabaseService.getProductById(product.getId());
        if(updated != null) setProduct(updated);
        else goBack();
    }
    private void setupBackHover() {
        lblBack.setOnMouseEntered(e -> lblBack.setStyle("-fx-text-fill: #333; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 14px;"));
        lblBack.setOnMouseExited(e -> lblBack.setStyle("-fx-text-fill: #888; -fx-cursor: hand; -fx-font-size: 14px;"));
    }



}