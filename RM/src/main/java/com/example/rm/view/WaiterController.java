package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import com.example.rm.view.screens.TakeOrderView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    @FXML
    public void initialize() {
        setupUserSession();
        setupHoverEffects();
        startPolling();
    }

    private void startPolling() {

        refreshNotifications();

        // Configura il refresh ogni 5 secondi
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            refreshNotifications();
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    //quando si esce dalla schermata per fermare il timer
    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
    }

    private void refreshNotifications() {

        List<Order> readyOrders = DatabaseService.getReadyOrdersForWaiter();
        notificationsContainer.getChildren().clear();
        UserSession session = UserSession.getInstance();

        for (Order order : readyOrders) {

            if (session.isTableManaged(order.getTavolo())) {
                notificationsContainer.getChildren().add(createNotificationCard(order));
        } }
    }

    private VBox createNotificationCard(Order order) {

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15; -fx-max-width: 300;");


        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.15));
        shadow.setRadius(10);
        shadow.setOffsetY(2);
        card.setEffect(shadow);


        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);


        Circle dot = new Circle(6, Color.web("#00E676"));
        Circle glow = new Circle(10, Color.web("#00E676", 0.4));
        glow.setEffect(new GaussianBlur(3));
        StackPane indicator = new StackPane(glow, dot);

        Label title = new Label("To deliver: table " + order.getTavolo());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");

        header.getChildren().addAll(indicator, title);


        VBox contentBox = new VBox(2);
        List<String> items = DatabaseService.getOrderItemsForDisplay(order.getId());

        // Mostriamo solo i primi 3 elementi per non allungare troppo la card, poi "..."
        int limit = 3;
        for (int i = 0; i < items.size(); i++) {
            if (i >= limit) {
                Label more = new Label("... (+" + (items.size() - limit) + " altri)");
                more.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
                contentBox.getChildren().add(more);
                break;
            }
            Label itemLbl = new Label(items.get(i));
            itemLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");
            itemLbl.setWrapText(true);
            contentBox.getChildren().add(itemLbl);
        }

        // --- FOOTER: Bottone Delivered ---
        Button btnDelivered = new Button("Delivered");
        btnDelivered.setMaxWidth(Double.MAX_VALUE);
        btnDelivered.setStyle(
                "-fx-background-color: #2B2B2B; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );

        // Azione Bottone
        btnDelivered.setOnAction(e -> {
            boolean success = DatabaseService.markOrderAsDelivered(order.getId());
            if (success) {
                // Rimuovi la card immediatamente dalla UI
                notificationsContainer.getChildren().remove(card);
                logger.info("Ordine " + order.getId() + " segnato come consegnato.");
            }
        });

        card.getChildren().addAll(header, contentBox, new Separator(), btnDelivered);
        return card;
    }

    // ... Resto dei metodi esistenti (setupUserSession, handleProfileMenu, handleNewOrder) ...
    // ... Assicurati di copiare i metodi esistenti qui sotto ...

    private void setupUserSession() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            lblHeaderName.setText(session.getUser().getUsername());
            lblHeaderRole.setText(session.getUser().getRole().toUpperCase());
            lblWelcomeTop.setText(session.getUser().getWelcomeMessage());
        }
    }

    private void setupHoverEffects() {
        if (profileBtn != null && profileCircle != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
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
        itemLogout.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        itemLogout.setOnAction(e -> {
            stopPolling(); // IMPORTANTE: ferma il timer al logout
            UserSession.cleanUserSession();
            try {
                Parent loginView = new FXMLLoader(getClass().getResource("/LoginView.fxml")).load();
                if (profileBtn.getScene() != null) profileBtn.getScene().setRoot(loginView);
            } catch (Exception ex) { logger.log(Level.SEVERE, "Logout error", ex); }
        });

        contextMenu.getItems().addAll(itemTableFilter, itemChangePassword, new SeparatorMenuItem(), itemLogout);
        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleNewOrder() {
        stopPolling(); // Ferma il polling quando si cambia schermata
        String input = txtTable.getText().trim();
        try {
            int tavoloSelezionato = Integer.parseInt(input);
            if (tavoloSelezionato <= 0) throw new NumberFormatException();
            txtTable.setStyle("-fx-border-color: #DDD;");

            if (btnNewOrder.getScene() != null) {
                View takeOrderView = new TakeOrderView(tavoloSelezionato);
                btnNewOrder.getScene().setRoot(takeOrderView.getRoot());
            }
        } catch (NumberFormatException ex) {
            txtTable.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        }
    }



    private void showTableFilterDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Filtro Tavoli");
        dialog.setHeaderText("Quali tavoli vuoi gestire oggi?");
        dialog.setContentText("Inserisci tavoli (es: 1-5; 10; 12):");

        // Opzionale: precompila con il valore attuale se esiste
        // dialog.getEditor().setText(...recupera stringa salvata...);

        dialog.showAndWait().ifPresent(input -> {
            // 1. Usa il parser creato al punto 1
            // (Assicurati di importare la classe TableSelectionUtils)
            java.util.Set<Integer> selectedTables = com.example.rm.util.TableSelectionUtils.parseTableString(input);

            // 2. Salva nella sessione
            UserSession.getInstance().setManagedTables(selectedTables);

            // 3. Ricarica immediata della vista per applicare il filtro
            refreshNotifications();

            logger.log(Level.INFO,"Filtro tavoli aggiornato: ", input);
        });
    }
}