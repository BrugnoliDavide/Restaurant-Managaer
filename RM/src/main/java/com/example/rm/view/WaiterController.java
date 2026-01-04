package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.view.screens.TakeOrderView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.geometry.Side;
import javafx.scene.input.MouseEvent;

public class WaiterController {

    private static final Logger logger = Logger.getLogger(WaiterController.class.getName());

    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeTop;
    @FXML private Button btnNewOrder;
    @FXML private TextField txtTable;

    @FXML
    public void initialize() {
        setupUserSession();
        setupHoverEffects();

        if (profileBtn != null) {
            Tooltip t = new Tooltip("Clicca per Logout");
            t.setShowDelay(Duration.millis(50));
            Tooltip.install(profileBtn, t);
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

    private void setupHoverEffects() {
        if (profileBtn != null && profileCircle != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }
    }

    @FXML
    private void handleProfileMenu(MouseEvent event) {

        ContextMenu contextMenu = new ContextMenu();

        // === OPZIONE 1: CAMBIA PASSWORD ===
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

        // === OPZIONE 2: LOGOUT ===
        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: red; " +
                        "-fx-font-weight: bold;"
        );
        itemLogout.setOnAction(e -> {
            logger.log(Level.INFO, "Logout Cameriere effettuato.");
            UserSession.cleanUserSession();

            try {
                Parent loginView = new FXMLLoader(
                        getClass().getResource("/LoginView.fxml")
                ).load();

                if (profileBtn.getScene() != null) {
                    profileBtn.getScene().setRoot(loginView);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore durante il logout", ex);
            }
        });

        // === ASSEMBLA MENU ===
        contextMenu.getItems().addAll(
                itemChangePassword,
                new SeparatorMenuItem(),
                itemLogout
        );

        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }



    @FXML
    private void handleNewOrder() {
        String input = txtTable.getText().trim();
        try {
            int tavoloSelezionato = Integer.parseInt(input);
            if (tavoloSelezionato <= 0) throw new NumberFormatException();

            txtTable.setStyle("-fx-border-color: #DDD;"); // Reset stile

            if (btnNewOrder.getScene() != null) {
                // Navigazione verso la presa ordine
                View takeOrderView = new TakeOrderView(tavoloSelezionato);
                btnNewOrder.getScene().setRoot(takeOrderView.getRoot());
            }

        } catch (NumberFormatException ex) {
            txtTable.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            logger.warning("Tavolo non valido: " + input);
        }
    }
}