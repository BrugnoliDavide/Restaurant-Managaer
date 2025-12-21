package com.example.demo.view;

import com.example.demo.app.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip; // Importante
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;
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

        // --- CORREZIONE: Creazione Tooltip da Codice ---
        // Poiché StackPane non supporta <tooltip> in FXML, lo installiamo qui
        if (profileBtn != null) {
            Tooltip t = new Tooltip("Logout");
            t.setShowDelay(Duration.millis(50)); // Appare subito
            Tooltip.install(profileBtn, t);      // Lo attacca allo StackPane
        }
    }

    private void setupUserSession() {
        UserSession session = UserSession.getInstance();

        String displayName = "Utente";
        String displayRole = "Ruolo";
        String welcomeMsg = "Welcome";

        if (session != null && session.getUser() != null) {
            com.example.demo.model.User u = session.getUser();
            displayName = u.getUsername();
            displayRole = u.getRole();
            welcomeMsg = u.getWelcomeMessage();
        }

        lblHeaderName.setText(displayName);
        lblHeaderRole.setText(displayRole);
        lblWelcomeTop.setText(welcomeMsg);

        logger.info("Waiter View inizializzata per: " + displayName);
    }

    private void setupHoverEffects() {
        if (profileBtn != null && profileCircle != null) {
            profileBtn.setOnMouseEntered(e -> {
                profileCircle.setStroke(Color.TOMATO);
                profileCircle.setStrokeWidth(3);
            });
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }
    }

    @FXML
    private void handleLogout() {
        String currentUser = lblHeaderName.getText();
        logger.log(Level.INFO, "Eseguo Logout da: " + currentUser);

        UserSession.cleanUserSession();

        try {
            Parent loginView = new FXMLLoader(getClass().getResource("/LoginView.fxml")).load();
            if (profileBtn.getScene() != null) {
                profileBtn.getScene().setRoot(loginView);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossibile caricare LoginView dopo il logout", e);
        }
    }

    @FXML
    private void handleNewOrder() {
        String input = txtTable.getText().trim();
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #DDD; -fx-border-radius: 10; -fx-min-width: 200; -fx-font-size: 14px;";
        String errorStyle  = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: red; -fx-border-radius: 10; -fx-min-width: 200; -fx-font-size: 14px;";

        try {
            int tavoloSelezionato = Integer.parseInt(input);
            if (tavoloSelezionato <= 0) throw new NumberFormatException();

            txtTable.setStyle(normalStyle);
            logger.log(Level.INFO, "Apro menu per tavolo: " + tavoloSelezionato);

            // Navigazione (Assicurati che TakeOrderView esista)
            if (btnNewOrder.getScene() != null) {
                btnNewOrder.getScene().setRoot(TakeOrderView.getView(tavoloSelezionato));
            }

        } catch (NumberFormatException ex) {
            txtTable.setStyle(errorStyle);
            logger.warning("Tentativo inserimento tavolo non valido: '" + input + "'");
        }
    }

    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(WaiterController.class.getResource("/WaiterView.fxml")).load();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento WaiterView.fxml", e);
            throw new RuntimeException(e);
        }
    }
}