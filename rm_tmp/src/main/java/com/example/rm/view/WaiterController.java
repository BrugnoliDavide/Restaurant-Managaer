package com.example.rm.view;

import com.example.rm.app.SceneManager;
import com.example.rm.app.UserSession;
import com.example.rm.controller.OrderService;
import com.example.rm.controller.OrderUseCase;
import com.example.rm.model.Order;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.geometry.Side;
import javafx.scene.input.MouseEvent;
import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WaiterController {

    public static final Logger logger = Logger.getLogger(WaiterController.class.getName());

    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeTop;
    @FXML private Button btnNewOrder;
    @FXML private TextField txtTable;
    @FXML private VBox notificationsContainer;

    private Timeline pollingTimeline;
    private static final OrderUseCase orderUseCase = new OrderService();

    @FXML
    public void initialize() {
        setupUserSession();
        startPolling();
    }

    private void startPolling() {
        refreshNotifications();

        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5),
                event ->refreshNotifications()));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
    }

    private void refreshNotifications() {
        List<Order> readyOrders = orderUseCase.loadReadyOrdersForWaiter();
        notificationsContainer.getChildren().clear();
        UserSession session = UserSession.getInstance();

        for (Order order : readyOrders) {
            if (session.isTableManaged(order.getTavolo())) {
                notificationsContainer.getChildren().add(createNotificationCard(order));
            }
        }
    }

    private VBox createNotificationCard(Order order) {
        VBox card = new VBox(10);
        card.getStyleClass().add("notification-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(6);
        dot.getStyleClass().add("notification-dot");

        Circle glow = new Circle(10);
        glow.getStyleClass().add("notification-glow");

        StackPane indicator = new StackPane(glow, dot);

        Label title = new Label("To deliver: table " + order.getTavolo());
        title.getStyleClass().add("notification-title");

        header.getChildren().addAll(indicator, title);

        VBox contentBox = new VBox(2);
        List<String> items = orderUseCase.loadOrderItemsForDisplay(order.getId());

        int limit = 3;
        for (int i = 0; i < items.size(); i++) {
            if (i >= limit) {
                Label more = new Label("... (+" + (items.size() - limit) + " altri)");
                more.getStyleClass().add("notification-more");
                contentBox.getChildren().add(more);
                break;
            }
            Label itemLbl = new Label(items.get(i));
            itemLbl.getStyleClass().add("notification-item");
            itemLbl.setWrapText(true);
            contentBox.getChildren().add(itemLbl);
        }

        Button btnDelivered = new Button("Delivered");
        btnDelivered.setMaxWidth(Double.MAX_VALUE);
        btnDelivered.getStyleClass().add("btn-delivered");

        btnDelivered.setOnAction(e -> {
            boolean success = orderUseCase.markOrderAsDelivered(order.getId());
            if (success) {
                notificationsContainer.getChildren().remove(card);
                logger.info("Ordine " + order.getId() + " segnato come consegnato.");
            }
        });

        card.getChildren().addAll(header, contentBox, new Separator(), btnDelivered);
        return card;
    }

    private void setupUserSession() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            lblHeaderName.setText(session.getUser().getUsername());
            lblHeaderRole.setText(session.getUser().getRole().toUpperCase());
            lblWelcomeTop.setText(session.getUser().getWelcomeMessage());
        }
    }

    @FXML
    private void handleProfileMenu(MouseEvent event) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemTableFilter = new MenuItem("Gestione Tavoli");
        itemTableFilter.setOnAction(e -> showTableFilterDialog());

        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.setOnAction(e -> ChangePasswordDialog.show((Stage) profileBtn.getScene().getWindow()));

        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.getStyleClass().add("context-menu-item-danger");
        itemLogout.setOnAction(e -> {
            stopPolling();
            UserSession.cleanUserSession();
            try {
                SceneManager.showLogin();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Logout error", ex);
            }
        });

        contextMenu.getItems().addAll(itemTableFilter, itemChangePassword, new SeparatorMenuItem(), itemLogout);
        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleNewOrder() {
        stopPolling();
        String input = txtTable.getText().trim();
        try {
            int tavoloSelezionato = Integer.parseInt(input);
            if (tavoloSelezionato <= 0) throw new NumberFormatException();

            txtTable.getStyleClass().remove("input-error");

            if (btnNewOrder.getScene() != null) {
                SceneManager.showTakeOrder(tavoloSelezionato);
            }
        } catch (NumberFormatException ex) {
            txtTable.getStyleClass().add("input-error");
        }
    }

    private void showTableFilterDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Filtro Tavoli");
        dialog.setHeaderText("Quali tavoli vuoi gestire oggi?");
        dialog.setContentText("Inserisci tavoli (es: 1-5; 10; 12):");

        dialog.showAndWait().ifPresent(input -> {
            java.util.Set<Integer> selectedTables = com.example.rm.util.TableSelectionUtils.parseTableString(input);
            UserSession.getInstance().setManagedTables(selectedTables);
            refreshNotifications();
            logger.log(Level.INFO, "Filtro tavoli aggiornato seguendo: {0}", input);
        });
    }
}