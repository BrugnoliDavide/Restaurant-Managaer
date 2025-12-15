package com.example.demo.view;

import com.example.demo.service.DatabaseService;
import com.example.demo.model.MenuProduct;
import com.example.demo.model.OrderItem;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TakeOrderView {


    private final static Map<Integer, OrderItem> carrello = new HashMap<>();

    public static Parent getView(int numeroTavolo) {
        carrello.clear();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFFFF;");

        // --- 1. HEADER ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(2);
        Label lblSubtitle = new Label("Waiter");
        lblSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");

        Label lblTitle = new Label("Select item (Tav. " + numeroTavolo + ")");
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        titles.getChildren().addAll(lblSubtitle, lblTitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCancel = new Button(" Cancel");
        SVGPath iconX = new SVGPath();
        iconX.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        iconX.setFill(Color.WHITE);
        iconX.setScaleX(0.8); iconX.setScaleY(0.8);

        btnCancel.setGraphic(iconX);
        btnCancel.setStyle("-fx-background-color: #E02E2E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 15 8 15;");

        btnCancel.setOnAction(e -> {
            System.out.println("Ordine annullato.");
            btnCancel.getScene().setRoot(WaiterView.getView());
        });

        header.getChildren().addAll(titles, spacer, btnCancel);
        root.setTop(header);


        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: transparent;");

        VBox listContent = new VBox();
        listContent.setPadding(new Insets(20, 0, 80, 0));

        List<MenuProduct> prodotti = DatabaseService.getAllProducts();

        for (MenuProduct p : prodotti) {
            HBox row = createProductRow(p);
            listContent.getChildren().add(row);
        }

        scrollPane.setContent(listContent);
        root.setCenter(scrollPane);

        // --- 3. BOTTONE SEND ---
        StackPane bottomPane = new StackPane();
        bottomPane.setPadding(new Insets(0, 0, 10, 0));
        bottomPane.setAlignment(Pos.BOTTOM_CENTER);

        Button btnSend = new Button("  Send Order");
        SVGPath iconUp = new SVGPath();
        iconUp.setContent("M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z");
        iconUp.setFill(Color.WHITE);
        btnSend.setGraphic(iconUp);

        btnSend.setStyle(
                "-fx-background-color: #2B2B2B; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 12; -fx-min-width: 250; -fx-min-height: 50; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);"
        );

        btnSend.setOnAction(e -> {
            if (carrello.isEmpty()) {
                showAlert("Carrello Vuoto", "Seleziona almeno un prodotto.");
                return;
            }
            List<OrderItem> itemsDaSalvare = new ArrayList<>(carrello.values());
            boolean success = DatabaseService.createOrder(itemsDaSalvare, numeroTavolo, "");

            if (success) {
                showAlert("Successo", "Ordine inviato in cucina!");
                btnSend.getScene().setRoot(WaiterView.getView());
            } else {
                showAlert("Errore", "Impossibile inviare ordine.");
            }
        });

        bottomPane.getChildren().add(btnSend);
        root.setBottom(bottomPane);

        return root;
    }

    // --- HELPER CREAZIONE RIGA CON LOGICA +/- ---
    private static HBox createProductRow(MenuProduct p) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 10, 15, 10));

        String baseStyle = "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0; -fx-background-color: white;";
        String flashGreen = "-fx-border-color: #4CAF50; -fx-border-width: 0 0 1 0; -fx-background-color: #E8F5E9;"; // Verde Chiaro
        String flashRed = "-fx-border-color: #E02E2E; -fx-border-width: 0 0 1 0; -fx-background-color: #FFEBEE;"; // Rosso Chiaro

        row.setStyle(baseStyle);

        // Info Prodotto
        VBox info = new VBox(4);
        Label name = new Label(p.getNome());
        name.setStyle("-fx-font-size: 16px; -fx-text-fill: #222222; -fx-font-weight: bold;");

        Label price = new Label(String.format("%.2f€", p.getPrezzoVendita()));
        price.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");

        info.getChildren().addAll(name, price);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- AZIONI (CONTROLS) ---
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // 1. TASTO MENO (-)
        Label btnMinus = new Label("−"); // Simbolo meno
        btnMinus.setVisible(false); // Inizialmente nascosto
        btnMinus.setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #E02E2E; -fx-padding: 0 5 0 0;");

        // 2. BADGE QUANTITÀ
        Label lblQtyBadge = new Label("x0");
        lblQtyBadge.setVisible(false);
        lblQtyBadge.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-background-radius: 10;"
        );

        // 3. TASTO PIU (+)
        Label btnAdd = new Label("+");
        btnAdd.setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #2B2B2B;");

        // --- LOGICA CLICK TASTO MENO ---
        btnMinus.setOnMouseClicked(e -> {
            e.consume(); // Evita che il click si propaghi alla riga (che farebbe "Add")

            int currentQty = decreaseQty(p); // Metodo helper per ridurre

            if (currentQty > 0) {
                // Aggiorna Badge
                lblQtyBadge.setText("x" + currentQty);
                // Flash Rosso leggero
                row.setStyle(flashRed);
            } else {
                // Rimosso completamente
                lblQtyBadge.setVisible(false);
                btnMinus.setVisible(false);
                row.setStyle(flashRed); // Flash rosso per indicare rimozione
            }

            // Ripristina stile dopo 150ms
            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(ev -> row.setStyle(baseStyle));
            pause.play();
        });

        // --- LOGICA CLICK TASTO PIU ---
        Runnable addAction = () -> {
            addToCart(p);
            int newQty = carrello.get(p.getId()).getQuantita();

            lblQtyBadge.setText("x" + newQty);
            lblQtyBadge.setVisible(true);
            btnMinus.setVisible(true); // Ora che c'è almeno 1, mostriamo il meno

            row.setStyle(flashGreen);
            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(ev -> row.setStyle(baseStyle));
            pause.play();
        };

        // Click sul Più
        btnAdd.setOnMouseClicked(e -> {
            e.consume(); // Blocca propagazione
            addAction.run();
        });

        // Click sull'intera riga (comodità per aggiungere)
        row.setOnMouseClicked(e -> addAction.run());

        actions.getChildren().addAll(btnMinus, lblQtyBadge, btnAdd);
        row.getChildren().addAll(info, spacer, actions);
        return row;
    }

    // Aumenta quantità
    private static void addToCart(MenuProduct p) {
        if (carrello.containsKey(p.getId())) {
            OrderItem vecchio = carrello.get(p.getId());
            carrello.put(p.getId(), new OrderItem(p, vecchio.getQuantita() + 1));
        } else {
            carrello.put(p.getId(), new OrderItem(p, 1));
        }
    }

    // Diminuisce quantità e ritorna il nuovo valore
    private static int decreaseQty(MenuProduct p) {
        if (carrello.containsKey(p.getId())) {
            OrderItem vecchio = carrello.get(p.getId());
            int nuovaQta = vecchio.getQuantita() - 1;

            if (nuovaQta <= 0) {
                carrello.remove(p.getId());
                return 0;
            } else {
                carrello.put(p.getId(), new OrderItem(p, nuovaQta));
                return nuovaQta;
            }
        }
        return 0;
    }

    private static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}