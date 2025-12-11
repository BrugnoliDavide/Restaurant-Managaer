package com.example.demo.view;

import com.example.demo.view.component.AddProductDialog;
import com.example.demo.service.DatabaseService;
import com.example.demo.model.MenuProduct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

public class ProductDetailView {

    public static Parent getView(MenuProduct product) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        // --- HEADER ---
        VBox header = new VBox(5);

        // Tasto Indietro
        Label lblBack = new Label("← Menu");
         lblBack.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-cursor: hand;");

// 2. Effetto Hover (Passaggio del mouse)
// Quando il mouse entra: diventa scuro e sottolineato
        lblBack.setOnMouseEntered(e ->
                lblBack.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: true;")
        );

// Quando il mouse esce: torna grigio e senza sottolineatura
        lblBack.setOnMouseExited(e ->
                lblBack.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: false;")
        );

// 3. Azione (Torna alla lista Menu)
        lblBack.setOnMouseClicked(e ->  lblBack.getScene().setRoot(MenuView.getView()));




        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(product.getNome());
        lblName.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #222;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Icona Cestino (DELETE)
        StackPane btnDelete = createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        btnDelete.setOnMouseClicked(e -> {
            boolean success = DatabaseService.deleteProduct(product.getId());
            if(success) lblBack.getScene().setRoot(MenuView.getView()); // Se cancellato, torna al menu
        });

        // Icona Matita (EDIT)
        StackPane btnEdit = createIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        btnEdit.setOnMouseClicked(e -> {
            AddProductDialog.displayEdit(product);
            // Ricarica questa pagina per vedere le modifiche
            // (Nota: in un'app reale ricaricheremmo i dati dal DB, qui ricarichiamo la view Menu per semplicità)
            lblBack.getScene().setRoot(MenuView.getView());
        });

        titleRow.getChildren().addAll(lblName, spacer, btnDelete, btnEdit);
        header.getChildren().addAll(lblBack, titleRow);
        root.setTop(header);

        // --- BODY (Dati) ---
        VBox content = new VBox(30);
        content.setPadding(new Insets(30, 0, 0, 0));

        // Calcoli
        long qta = DatabaseService.getQuantitySold(product.getNome());
        double income = (product.getPrezzoVendita() - product.getCostoRealizzazione()) * qta;

        content.getChildren().add(createRow("Price", "", String.format("%.2f€", product.getPrezzoVendita())));
        content.getChildren().add(createRow("Sold Quantity", "quantities sold last 30 days", String.valueOf(qta)));
        content.getChildren().add(createRow("Realization Price", "", String.format("%.2f€", product.getCostoRealizzazione())));
        content.getChildren().add(createRow("Realized Income", "Total profit", String.format("%.2f€", income)));

        root.setCenter(content);
        return root;
    }

    private static HBox createRow(String title, String sub, String val) {
        HBox row = new HBox();
        row.setAlignment(Pos.TOP_LEFT);

        VBox labels = new VBox(2);
        Label t = new Label(title); t.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");
        Label s = new Label(sub); s.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
        labels.getChildren().addAll(t, s);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label v = new Label(val); v.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        row.getChildren().addAll(labels, spacer, v);
        return row;
    }

    private static StackPane createIcon(String pathData) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setScaleX(1.4); path.setScaleY(1.4);
        StackPane p = new StackPane(path);
        p.setPrefSize(30, 30);
        p.setStyle("-fx-cursor: hand;");
        return p;
    }
}