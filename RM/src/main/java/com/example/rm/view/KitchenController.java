package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.Order;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;
import com.example.rm.view.component.KitchenPreferencesDialog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;

import static com.example.rm.view.LoginController.logger;import java.io.UncheckedIOException;


public class KitchenController {


    @FXML private VBox ordersContainer;
    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeMsg;
    @FXML private Label lblActiveFilters;


    @FXML
    public void initialize() {

        // Recupero sessione
        UserSession session = UserSession.getInstance();

        // Controlliamo che sessione e utente esistano per evitare crash
        if (session != null && session.getUser() != null) {
            // Qui recuperiamo l'oggetto USER vero e proprio
            com.example.rm.model.User currentUser = session.getUser();

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


    @FXML
    private void handleProfileMenu(MouseEvent event) {

        ContextMenu contextMenu = new ContextMenu();


        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.setStyle(
                "-fx-font-size: 14px; " +
                    "-fx-padding: 5 10 5 10; " +
                    "-fx-text-fill: #2196F3;"
        );
        itemChangePassword.setOnAction(e -> {
            Stage stage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(stage);
        });


        MenuItem itemPreference = new MenuItem("Preference");
        itemPreference.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: #2196F3;"
        );
        itemPreference.setOnAction(e -> {

            showPreferencesDialog();
        });




        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: red; " +
                        "-fx-font-weight: bold;"
        );
        itemLogout.setOnAction(e -> {
            logger.log(Level.INFO, "Logout Cucina effettuato.");
            UserSession.cleanUserSession();

            try {
                Parent loginView = new FXMLLoader(
                        getClass().getResource("/LoginView.fxml")
                ).load();

                if (profileBtn.getScene() != null) {
                    profileBtn.getScene().setRoot(loginView);
                }
            } catch (IOException ex) {
                logger.log(
                        Level.SEVERE,
                        "Errore apertura menu profilo",
                        ex
                );
            }
        });



        contextMenu.getItems().addAll(
                itemChangePassword,
                new SeparatorMenuItem(),
                itemPreference,  // ← AGGIUNGI QUESTA RIGA
                new SeparatorMenuItem(),
                itemLogout
        );

        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }



    @FXML
    public void refreshData() {
        ordersContainer.getChildren().clear();

        UserSession session = UserSession.getInstance();
        String username = (session != null && session.getUser() != null)
                ? session.getUser().getUsername()
                : "guest";

        System.out.println("===== REFRESH DATA =====");
        System.out.println("Username: " + username);

        KitchenPreferences prefs = DatabaseService.getKitchenPreferences(username);

        System.out.println("Preferenze caricate:");
        System.out.println("  - splitOrders: " + prefs.isSplitMixedCategoryOrders());
        System.out.println("  - selectedCategories: " + prefs.getSelectedCategories());
        System.out.println("  - includeOther: " + prefs.isIncludeOtherCategories());

        if (prefs.isSplitMixedCategoryOrders()) {
            List<Order> allOrders = DatabaseService.getKitchenActiveOrders();
            System.out.println("Ordini da scomporre: " + allOrders.size());
            for (Order order : allOrders) {
                DatabaseService.decomposeOrderIfNeeded(order.getId());
            }
        }

        if (prefs.isIncludeOtherCategories()) {
            lblActiveFilters.setText("Filtri attivi: TUTTE le categorie (incluso 'Altro')");
        } else if (prefs.getSelectedCategories().isEmpty()) {
            lblActiveFilters.setText("Filtri attivi: Nessuna categoria selezionata");
        } else {
            String categoriesText = String.join(", ", prefs.getSelectedCategories());
            lblActiveFilters.setText("Filtri attivi: " + categoriesText);
        }

        // ✅ AGGIUNGI QUESTO LOG
        System.out.println("Recuperando ordini filtrati per: " + username);
        List<Order> activeOrders = DatabaseService.getKitchenActiveOrdersFiltered(username);
        System.out.println("Ordini filtrati recuperati: " + activeOrders.size());

        if (activeOrders.isEmpty()) {
            Label empty = new Label("Nessun ordine in attesa.");
            empty.setStyle("-fx-font-size: 18px; -fx-text-fill: #999; -fx-padding: 20;");
            ordersContainer.getChildren().add(empty);
            return;
        }

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
            DatabaseService.setOrderStatus(order.getId(), "ready");
            // Rimuovi visivamente la card
            ordersContainer.getChildren().remove(card);
        });

        // Assembla la card
        card.getChildren().addAll(leftInfo, btnDone);

        return card;
    }

    /* !! deprecato
     public static Parent getFXMLView() {
        try {
            return new FXMLLoader(KitchenController.class.getResource("/KitchenView.fxml")).load();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile caricare KitchenView.fxml. Controlla che il file esista in resources.", e);
        }
    }*/

    private void showPreferencesDialog() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            String username = session.getUser().getUsername();
            Stage stage = (Stage) profileBtn.getScene().getWindow();

            KitchenPreferencesDialog.show(stage, username, success -> {
                if (success) {
                    logger.log(Level.INFO, "Preferenze cucina aggiornate per " + username);
                    // ✅ RICARICA GLI ORDINI SUBITO
                    refreshData();
                }
            });
        }
    }


/*
    @FXML
    public void refreshData() {
        ordersContainer.getChildren().clear();

        UserSession session = UserSession.getInstance();
        String username = (session != null && session.getUser() != null)
                ? session.getUser().getUsername()
                : "guest";

        // Carica preferenze
        KitchenPreferences prefs = DatabaseService.getKitchenPreferences(username);

        // ✅ SE splitOrders è true, scomponi gli ordini PRIMA di recuperarli
        if (prefs.isSplitMixedCategoryOrders()) {
            List<Order> allOrders = DatabaseService.getKitchenActiveOrders();
            for (Order order : allOrders) {
                DatabaseService.decomposeOrderIfNeeded(order.getId());
            }
        }

        // Mostra filtri attivi
        if (prefs.isIncludeOtherCategories()) {
            lblActiveFilters.setText("Filtri attivi: TUTTE le categorie (incluso 'Altro')");
        } else if (prefs.getSelectedCategories().isEmpty()) {
            lblActiveFilters.setText("Filtri attivi: Nessuna categoria selezionata");
        } else {
            String categoriesText = String.join(", ", prefs.getSelectedCategories());
            lblActiveFilters.setText("Filtri attivi: " + categoriesText);
        }

        // Recupera ordini FILTRATI
        List<Order> activeOrders = DatabaseService.getKitchenActiveOrdersFiltered(username);

        if (activeOrders.isEmpty()) {
            Label empty = new Label("Nessun ordine in attesa.");
            empty.setStyle("-fx-font-size: 18px; -fx-text-fill: #999; -fx-padding: 20;");
            ordersContainer.getChildren().add(empty);
            return;
        }

        for (Order order : activeOrders) {
            HBox card = createOrderCard(order);
            ordersContainer.getChildren().add(card);
        }
    }

*/

}