package com.example.demo.view;

import com.example.demo.app.UserSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration; // Importante per la velocità del Tooltip

public class WaiterView {

    // Placeholder Dati Utente
    private final static String userName = "Sir. Robert";
    private final static String userRole = "Waiter";
    private final static Color profilePlaceholderColor = Color.CORNFLOWERBLUE;

    public static Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Applica sfondo tema
        root.getStyleClass().add("view-background");

        // --- 1. HEADER (Top) ---
        HBox header = createHeader();
        root.setTop(header);

        // --- 2. CENTER (Spazio vuoto) ---
        StackPane center = new StackPane();
        root.setCenter(center);

        // --- 3. BOTTOM (Controlli Ordine) ---
        VBox bottomBox = createBottomControls();
        root.setBottom(bottomBox);

        return root;
    }

    private static HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // 1. Cerchio Immagine
        Circle profileImg = new Circle(25, profilePlaceholderColor);

        // 2. Container Bottone (StackPane)
        StackPane profileBtn = new StackPane(profileImg);
        profileBtn.setStyle("-fx-cursor: hand;");

        // --- TOOLTIP (La scritta Logout) ---
        Tooltip tooltip = new Tooltip("Logout");
        // FONDAMENTALE: Fa apparire la scritta quasi subito (50ms) invece di aspettare 1 secondo
        tooltip.setShowDelay(Duration.millis(50));
        Tooltip.install(profileBtn, tooltip);

        // 3. Feedback Visivo (Bordo Rosso)
        profileBtn.setOnMouseEntered(e -> {
            profileImg.setStroke(Color.TOMATO);
            profileImg.setStrokeWidth(3);
        });
        profileBtn.setOnMouseExited(e -> {

            profileImg.setStroke(null);
        });

        // 4. Azione Logout
        profileBtn.setOnMouseClicked(e -> {
            UserSession.cleanUserSession();
            System.out.println("Eseguo Logout...");
            // Ora chiami il nuovo metodo statico che gestisce il caricamento FXML
            profileBtn.getScene().setRoot(LoginController.getFXMLView());
        });

        // Testi
        VBox texts = new VBox(2);
        Label nameLbl = new Label(userName);
        nameLbl.getStyleClass().add("text-title");
        nameLbl.setStyle("-fx-font-size: 16px;");

        Label roleLbl = new Label(userRole);
        roleLbl.getStyleClass().add("text-body");
        roleLbl.setStyle("-fx-font-size: 14px;");

        texts.getChildren().addAll(nameLbl, roleLbl);

        // Spaziatore
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Icona Orologio
        StackPane clockIcon = createIcon("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z");

        // *** CORREZIONE CRUCIALE: Qui aggiungiamo profileBtn (che ha il tooltip), NON profileImg ***
        header.getChildren().addAll(profileBtn, texts, spacer, clockIcon);

        return header;
    }

    private static VBox createBottomControls() {
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20, 0, 10, 0));

        // Bottone "New Order"
        Button btnNewOrder = new Button("new\norder");
        btnNewOrder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        btnNewOrder.setStyle(
                "-fx-background-color: #2B2B2B;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 140;" +
                        "-fx-min-height: 60;" +
                        "-fx-cursor: hand;"
        );

        Label lblTable = new Label("table");
        lblTable.getStyleClass().add("text-body");
        lblTable.setStyle("-fx-font-size: 14px;");

        TextField txtTable = new TextField();
        txtTable.setPromptText("number");
        txtTable.setAlignment(Pos.CENTER);

        String normalStyle = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #DDD; -fx-border-radius: 10; -fx-min-width: 200; -fx-font-size: 14px;";
        String errorStyle  = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: red; -fx-border-radius: 10; -fx-min-width: 200; -fx-font-size: 14px;";

        txtTable.setStyle(normalStyle);

        btnNewOrder.setOnAction(e -> {
            String input = txtTable.getText().trim();
            try {
                int tavoloSelezionato = Integer.parseInt(input);
                if (tavoloSelezionato <= 0) throw new NumberFormatException();

                txtTable.setStyle(normalStyle);
                System.out.println("Apro menu per tavolo: " + tavoloSelezionato);
                btnNewOrder.getScene().setRoot(TakeOrderView.getView(tavoloSelezionato));

            } catch (NumberFormatException ex) {
                txtTable.setStyle(errorStyle);
            }
        });

        layout.getChildren().addAll(btnNewOrder, lblTable, txtTable);
        return layout;
    }

    private static StackPane createIcon(String svgContent) {
        SVGPath path = new SVGPath();
        path.setContent(svgContent);
        path.getStyleClass().add("icon-svg");
        path.setScaleX(1.5);
        path.setScaleY(1.5);
        StackPane p = new StackPane(path);
        p.setPrefSize(40, 40);
        return p;
    }
}