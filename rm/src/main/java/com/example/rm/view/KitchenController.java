package com.example.rm.view;

import com.example.rm.app.SceneManager;
import com.example.rm.app.UserSession;
import com.example.rm.model.Order;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.preference.SimpleGraphicsManager;
import com.example.rm.view.component.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.example.rm.controller.KitchenService;
import com.example.rm.controller.KitchenUseCase;

public class KitchenController {

    @FXML private VBox ordersContainer;
    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeMsg;
    @FXML private Label lblActiveFilters;

    private static KitchenUseCase kitchenUseCase = new KitchenService();
    public static final Logger logger = Logger.getLogger(KitchenController.class.getName());

    public static void setKitchenUseCase(KitchenUseCase useCase) {
        kitchenUseCase = useCase;
    }

    private KitchenOrderCardFactory cardFactory;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();

        if (SimpleGraphicsManager.isEinkMode()){
            this.cardFactory = new KitchenOrderCardFactoryEink(kitchenUseCase);
        } else this.cardFactory = new KitchenOrderCardFactoryClassic(kitchenUseCase);


        if (session != null && session.getUser() != null) {
            com.example.rm.model.User currentUser = session.getUser();

            String username = currentUser.getUsername();
            String role = currentUser.getRole();

            lblHeaderName.setText(username);
            lblHeaderRole.setText(role.toUpperCase());
            lblWelcomeMsg.setText("Cucina operativa. Buon lavoro, " + username + "!");
        }

        refreshData();
    }

    @FXML
    private void handleProfileMenu(MouseEvent event) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.getStyleClass().add("context-menu-item-info");
        itemChangePassword.setOnAction(e -> {
            Stage stage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(stage);
        });

        MenuItem itemPreference = new MenuItem("Preference");
        itemPreference.getStyleClass().add("context-menu-item-info");
        itemPreference.setOnAction(e ->  showPreferencesDialog());

        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.getStyleClass().add("context-menu-item-danger");
        itemLogout.setOnAction(e -> {
            logger.log(Level.INFO, "Logout Cucina effettuato.");
            UserSession.cleanUserSession();
            SceneManager.showLogin();


        });

        contextMenu.getItems().addAll(
                itemChangePassword,
                new SeparatorMenuItem(),
                itemPreference,
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

        KitchenPreferences prefs = kitchenUseCase.loadPreferences(username);

        if (prefs.isSplitMixedCategoryOrders()) {
            kitchenUseCase.splitMixedOrdersIfNeeded();
        }

        if (prefs.isIncludeOtherCategories()) {
            lblActiveFilters.setText("Filtri attivi: TUTTE le categorie (incluso 'Altro')");
        } else if (prefs.getSelectedCategories().isEmpty()) {
            lblActiveFilters.setText("Filtri attivi: Nessuna categoria selezionata");
        } else {
            String categoriesText = String.join(", ", prefs.getSelectedCategories());
            lblActiveFilters.setText("Filtri attivi: " + categoriesText);
        }

        List<Order> activeOrders = kitchenUseCase.loadFilteredOrders(username);

        if (activeOrders.isEmpty()) {
            Label empty = new Label("Nessun ordine in attesa.");
            empty.getStyleClass().add("kitchen-empty-state");
            ordersContainer.getChildren().add(empty);
            return;
        }

        for (Order order : activeOrders) {

            final javafx.scene.Node[] cardRef = new javafx.scene.Node[1];

            cardRef[0] = cardFactory.createOrderCard(order, () -> {
                logger.info("Ordine #" + order.getId() + " completato.");
                kitchenUseCase.updateOrderStatus(order.getId(), "ready");
                ordersContainer.getChildren().remove(cardRef[0]);
            });

            ordersContainer.getChildren().add(cardRef[0]);
        }
    }

    private HBox createOrderCard(Order order) {
        HBox card = new HBox(20);
        card.getStyleClass().add("order-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox leftInfo = new VBox(5);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        String titleText = "Ordine #" + order.getId();
        if (order.getTavolo() > 0) titleText += " (Tavolo " + order.getTavolo() + ")";

        Label lblTitle = new Label(titleText);
        lblTitle.getStyleClass().add("order-title");

        Label lblTime = new Label("Arrivato alle: " + order.getDataOra().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.getStyleClass().add("order-time");

        VBox itemsBox = new VBox(2);
        itemsBox.setPadding(new Insets(10, 0, 0, 0));

        List<String> items = kitchenUseCase.getOrderItemsDisplay(order.getId());
        for (String itemStr : items) {
            Label itemLbl = new Label("• " + itemStr);
            itemLbl.getStyleClass().add("order-item");
            itemsBox.getChildren().add(itemLbl);
        }

        leftInfo.getChildren().addAll(lblTitle, lblTime, itemsBox);

        if (order.getNote() != null && !order.getNote().isEmpty()) {
            Label lblNote = new Label("NOTE: " + order.getNote());
            lblNote.getStyleClass().add("order-note-kitchen");
            leftInfo.getChildren().add(lblNote);
        }

        Button btnDone = new Button("PRONTO");
        btnDone.getStyleClass().add("btn-ready");

        btnDone.setOnAction(e -> {
            logger.info("Ordine #" + order.getId() + " completato.");
            kitchenUseCase.updateOrderStatus(order.getId(), "ready");

            ordersContainer.getChildren().remove(card);
        });

        card.getChildren().addAll(leftInfo, btnDone);

        return card;
    }

    private void showPreferencesDialog() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            String username = session.getUser().getUsername();
            Stage stage = (Stage) profileBtn.getScene().getWindow();

            KitchenPreferencesDialog.show(stage, username, success -> {
                if (Boolean.TRUE.equals(success)) {
                    logger.log(Level.INFO, "Preferenze cucina aggiornate per {0}", username);
                    refreshData();
                }
            });
        }
    }
}