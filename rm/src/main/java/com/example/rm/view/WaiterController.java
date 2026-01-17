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
import com.example.rm.preference.SimpleGraphicsManager;
import com.example.rm.view.component.WaiterNotificationCardFactory;
import com.example.rm.view.component.WaiterNotificationCardFactoryClassic;
import com.example.rm.view.component.WaiterNotificationCardFactoryEink;
import javafx.scene.Node;

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
    private static OrderUseCase orderUseCase = new OrderService();
    private WaiterNotificationCardFactory cardFactory;

    @FXML
    public void initialize() {
        setupUserSession();

        if (SimpleGraphicsManager.isEinkMode()) {
            this.cardFactory = new WaiterNotificationCardFactoryEink(orderUseCase);
        } else {
            this.cardFactory = new WaiterNotificationCardFactoryClassic(orderUseCase);
        }

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
                final Node[] cardRef = new Node[1];

                cardRef[0] = cardFactory.createNotificationCard(order, () -> {
                    boolean success = orderUseCase.markOrderAsDelivered(order.getId());
                    if (success) {
                        notificationsContainer.getChildren().remove(cardRef[0]);
                        logger.info("Ordine " + order.getId() + " segnato come consegnato.");
                    }
                });
                notificationsContainer.getChildren().add(cardRef[0]);
            }
        }
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