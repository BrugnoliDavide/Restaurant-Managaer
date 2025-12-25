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
    private void handleLogout() {
        logger.log(Level.INFO, "Eseguo Logout.");

        // 1. Pulisci la sessione
        UserSession.cleanUserSession();

        // 2. Torna al Login usando il ViewFactory (metodo più pulito)
        // Se ViewFactory non ha .login(), usa FXMLLoader come nel KitchenController
        try {
            if (profileBtn.getScene() != null) {
                // Carichiamo la View di login tramite ViewFactory o manualmente
                Parent loginView = new FXMLLoader(getClass().getResource("/LoginView.fxml")).load();
                // Otteniamo la scena attuale dal bottone profilo e cambiamo la root
                if (profileBtn.getScene() != null) {
                    profileBtn.getScene().setRoot(loginView);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il logout", e);
        }
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