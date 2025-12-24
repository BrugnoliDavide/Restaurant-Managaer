/*package com.example.demo.view;

import com.example.demo.view.component.AddProductDialog;
import com.example.demo.service.DatabaseService;
import com.example.demo.model.MenuProduct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.util.List;
import java.util.Map;

public class MenuView {

    public static Parent getView() {
        // --- 1. SETUP STRUTTURA ---
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #FFFFFF;"); // Sfondo bianco pulito

        // --- 2. HEADER (Intestazione) ---
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(20, 20, 10, 20));
        topContainer.setStyle("-fx-background-color: white;");

        // Tasto Indietro "Manage"
        Label lblManage = new Label("← Manage");
        lblManage.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-cursor: hand;");

        // Effetto hover sul tasto indietro
        lblManage.setOnMouseEntered(e -> lblManage.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: true;"));
        lblManage.setOnMouseExited(e -> lblManage.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: false;"));

        // Azione: Torna alla Dashboard Manager
        lblManage.setOnMouseClicked(e -> {
            View managerView = ViewFactory.forRole("manager");
            lblManage.getScene().setRoot(managerView.getRoot());
        });
        // Riga Titolo e Icone
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("Menu");
        lblTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Icona Cerca (Estetica)
        StackPane btnSearch = createIcon("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");

        // Icona Aggiungi (+)
        StackPane btnAdd = createIcon("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
        HBox.setMargin(btnAdd, new Insets(0, 0, 0, 15));

        // AZIONE AGGIUNGI: Apre il dialog, poi ricarica la pagina
        btnAdd.setOnMouseClicked(e -> {
            AddProductDialog.display();
            // Ricarica la vista corrente per mostrare il nuovo prodotto
            btnAdd.getScene().setRoot(MenuView.getView());
        });

        titleRow.getChildren().addAll(lblTitle, spacer, btnSearch, btnAdd);

        // Linea separatrice
        Separator sep = new Separator();
        sep.setPadding(new Insets(10, 0, 0, 0));

        topContainer.getChildren().addAll(lblManage, titleRow, sep);
        root.setTop(topContainer);


        // --- 3. LISTA PRODOTTI (Scrollabile) ---
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        // Rimuovi bordi blu di default e rendi sfondo trasparente/bianco
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: transparent;");

        VBox listContent = new VBox();
        listContent.setPadding(new Insets(10, 20, 20, 20));

        // --- RECUPERO DATI DAL DB ---
        // 1. Chiede al DB i prodotti raggruppati per categoria
        Map<String, List<MenuProduct>> datiMenu = DatabaseService.getMenuByCategories();

        // 2. Ciclo sulle categorie (es. "Primi", "Bibite")
        for (String categoria : datiMenu.keySet()) {

            // Intestazione Categoria
            Label sectionTitle = new Label(categoria);
            sectionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #999; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
            listContent.getChildren().add(sectionTitle);

            // 3. Ciclo sui prodotti di quella categoria
            for (MenuProduct prodotto : datiMenu.get(categoria)) {
                HBox row = createProductRow(prodotto);
                listContent.getChildren().add(row);
            }
        }

        scrollPane.setContent(listContent);
        root.setCenter(scrollPane);

        return root;
    }

    // --- METODI DI SUPPORTO ---

    // Crea la singola riga del prodotto
    private static HBox createProductRow(MenuProduct p) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 0, 15, 0));
        // Bordo grigio chiaro solo sotto
        row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white;");
        row.setCursor(javafx.scene.Cursor.HAND);

        // AZIONE CLICK RIGA: Vai ai dettagli (Modifica/Elimina completi)
        row.setOnMouseClicked(e -> {
            // Se l'utente ha cliccato sui "..." non aprire i dettagli
            if (e.getTarget() instanceof Label && ((Label)e.getTarget()).getText().equals("...")) return;

            // Cambia schermata verso ProductDetailView
            row.getScene().setRoot(ProductDetailView.getView(p));
        });

        // Effetto Hover sulla riga
        row.setOnMouseEntered(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #F9F9F9;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: white;"));

        // Parte Sinistra: Nome e Prezzo/Qta
        VBox infoBox = new VBox(5);

        Label nameLbl = new Label(p.getNome());
        nameLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

        long qtaVenduta = DatabaseService.getQuantitySold(p.getNome());

        Label detailsLbl = new Label(String.format("%.2f€ | Q.ta: %d", p.getPrezzoVendita(), qtaVenduta));
        detailsLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #777;");

        infoBox.getChildren().addAll(nameLbl, detailsLbl);

        // Spaziatore
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Parte Destra: Tre puntini (...) per eliminazione rapida
        Label dots = new Label("...");
        dots.setStyle("-fx-font-size: 20px; -fx-text-fill: #555; -fx-padding: 0 10 0 0; -fx-cursor: hand;");

        // Menu contestuale per eliminare
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Elimina Prodotto");
        deleteItem.setOnAction(e -> {
            boolean success = DatabaseService.deleteProduct(p.getId());
            if (success) {
                // Ricarica la pagina per vedere le modifiche
                dots.getScene().setRoot(MenuView.getView());
            }
        });
        contextMenu.getItems().add(deleteItem);

        // Apre il menu al click sui puntini
        dots.setOnMouseClicked(e -> contextMenu.show(dots, javafx.geometry.Side.BOTTOM, 0, 0));

        row.getChildren().addAll(infoBox, spacer, dots);
        return row;
    }

    // Crea icone SVG
    private static StackPane createIcon(String svgData) {
        SVGPath path = new SVGPath();
        path.setContent(svgData);
        path.setScaleX(1.2);
        path.setScaleY(1.2);
        path.setFill(Color.BLACK);

        StackPane container = new StackPane(path);
        container.setPrefSize(30, 30);
        container.setStyle("-fx-cursor: hand;");
        return container;
    }
}*/