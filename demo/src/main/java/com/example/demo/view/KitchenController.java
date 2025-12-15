package com.example.demo.view;

import com.example.demo.app.UserSession;
import com.example.demo.model.Order;
import com.example.demo.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.example.demo.view.LoginController.logger;

public class KitchenController {


    @FXML private VBox ordersContainer;


    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeMsg;

    @FXML
    public void initialize() {

        // Recupero sessione
        UserSession session = UserSession.getInstance();

        // Controlliamo che sessione e utente esistano per evitare crash
        if (session != null && session.getUser() != null) {
            // Qui recuperiamo l'oggetto USER vero e proprio
            com.example.demo.model.User currentUser = session.getUser();

            // Ora possiamo chiamare getUsername() e getRole() sull'oggetto User
            String username = currentUser.getUsername();
            String role = currentUser.getRole();

            // Impostiamo i testi nell'interfaccia
            lblHeaderName.setText(username);
            lblHeaderRole.setText(role.toUpperCase());
            lblWelcomeMsg.setText("Cucina operativa. Buon lavoro, " + username + "!");
        }


        if (profileBtn != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }

        refreshData();
    }

    // --- LOGICA LOGOUT ---
    @FXML
    private void handleProfileMenu(MouseEvent event) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        itemLogout.setOnAction(e -> {
            System.out.println("Logout Cucina effettuato.");

            // 1. Pulisci la sessione
            UserSession.cleanUserSession();

            // 2. Torna alla schermata di Login (Caricamento manuale)
            try {
                Parent loginView = new FXMLLoader(getClass().getResource("/LoginView.fxml")).load();
                // Otteniamo la scena attuale dal bottone profilo e cambiamo la root
                if (profileBtn.getScene() != null) {
                    profileBtn.getScene().setRoot(loginView);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        contextMenu.getItems().add(itemLogout);
        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }

    // --- GESTIONE ORDINI ---
    @FXML
    public void refreshData() {
        // Pulisce la lista visiva
        ordersContainer.getChildren().clear();

        // Recupera gli ordini dal Database
        List<Order> activeOrders = DatabaseService.getKitchenActiveOrders();

        // Se non ci sono ordini, mostra un messaggio
        if (activeOrders.isEmpty()) {
            Label empty = new Label("Nessun ordine in attesa.");
            empty.setStyle("-fx-font-size: 18px; -fx-text-fill: #999; -fx-padding: 20;");
            ordersContainer.getChildren().add(empty);
            return;
        }

        // Per ogni ordine trovato, crea la card grafica
        for (Order order : activeOrders) {
            HBox card = createOrderCard(order);
            ordersContainer.getChildren().add(card);
        }
    }

    private HBox createOrderCard(Order order) {

        HBox card = new HBox(20);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #DDD; -fx-border-width: 1; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);


        VBox leftInfo = new VBox(5);
        HBox.setHgrow(leftInfo, Priority.ALWAYS); // Occupa tutto lo spazio a sinistra


        String titleText = "Ordine #" + order.getId();
        if (order.getTavolo() > 0) titleText += " (Tavolo " + order.getTavolo() + ")";

        Label lblTitle = new Label(titleText);
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Orario
        Label lblTime = new Label("Arrivato alle: " + order.getDataOra().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        // Lista Piatti (Recuperati dal DB)
        VBox itemsBox = new VBox(2);
        itemsBox.setPadding(new Insets(10, 0, 0, 0));

        List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());
        for (String itemStr : items) {
            Label itemLbl = new Label("• " + itemStr);
            itemLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #444;");
            itemsBox.getChildren().add(itemLbl);
        }

        leftInfo.getChildren().addAll(lblTitle, lblTime, itemsBox);

        // Note (se presenti, in rosso)
        if (order.getNote() != null && !order.getNote().isEmpty()) {
            Label lblNote = new Label("NOTE: " + order.getNote());
            lblNote.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
            leftInfo.getChildren().add(lblNote);
        }

        // 3. Colonna Destra: Bottone Azione
        Button btnDone = new Button("PRONTO");
        btnDone.setPrefHeight(50);
        btnDone.setPrefWidth(100);
        btnDone.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;");

        // Effetto Hover sul bottone
        btnDone.setOnMouseEntered(e -> btnDone.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;"));
        btnDone.setOnMouseExited(e -> btnDone.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;"));

        // Azione Click: Segna come pronto
        btnDone.setOnAction(e -> {


            logger.info("Ordine #" + order.getId() + " completato.");
            // Aggiorna DB
            DatabaseService.setOrderStatus(order.getId(), "pronto");
            // Rimuovi visivamente la card
            ordersContainer.getChildren().remove(card);
        });

        // Assembla la card
        card.getChildren().addAll(leftInfo, btnDone);

        return card;
    }


    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(KitchenController.class.getResource("/KitchenView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare KitchenView.fxml. Controlla che il file esista in resources.", e);
        }
    }
}