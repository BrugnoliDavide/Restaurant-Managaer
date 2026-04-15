package com.example.rm.view;

import com.example.rm.app.AppStatus;
import com.example.rm.app.SceneManager;
import com.example.rm.app.UsersFactory;
import com.example.rm.app.UserSession;
import com.example.rm.service.ConnectionManager;
import com.example.rm.service.SecurityService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.example.rm.exception.AuthenticationException;

public class LoginController {

    @FXML private FontIcon gearIcon;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Circle dbStatusCircle;

    public static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML
    public void initialize() {
        updateDbStatusIndicator();

        if (gearIcon == null) {
            throw new IllegalStateException(
                    "gearIcon non è stato iniettato correttamente dal file FXML");
        }
        gearIcon.setOnMouseClicked(event -> openDBConfigPopup());
    }

    @FXML
    private void handleLogin() {
        resetStyle();

        boolean ok = ConnectionManager.testConnection();
        AppStatus.setDbConnectionOk(ok);
        updateDbStatusIndicator();

        if (!ok) {
            String error = ConnectionManager.getLastConnectionError();
            logger.log(Level.WARNING, "Login bloccato: DB non raggiungibile - {0}", error);
            showError();

            // Mostra tooltip con il dettaglio dell'errore sul cerchio di stato
            if (error != null && dbStatusCircle != null) {
                Tooltip errorTooltip = new Tooltip(error);
                errorTooltip.setWrapText(true);
                errorTooltip.setMaxWidth(300);
                Tooltip.install(dbStatusCircle, errorTooltip);
            }
            return;
        }

        String user = userField.getText() != null ? userField.getText().trim() : "";
        String pass = passField.getText() != null ? passField.getText().trim() : "";

        logger.log(Level.INFO, "Tentativo di autenticazione per username: {0}", user);

        if (user.isEmpty() || pass.isEmpty()) {
            showError();
            return;
        }

        try {
            String role = SecurityService.authenticate(user, pass);
            if (role != null) {
                logger.log(Level.INFO,
                        "Login COMPLETATO per utente: {0} [Ruolo assegnato: {1}]",
                        new Object[]{user, role});

                com.example.rm.model.User currentUser =
                        UsersFactory.createUser(user, role);
                UserSession.getInstance(currentUser);
                navigateToRole(role.toLowerCase());
            }
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "Login fallito: {0}", e.getUserMessage());
            showError();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore imprevisto durante il login", e);
            showError();
        }
    }

    private void showError() {
        String errorStyle = "-fx-border-color: #E02E2E; -fx-border-width: 2px; "
                + "-fx-border-radius: 5px; -fx-background-radius: 5px;";
        userField.setStyle(errorStyle);
        passField.setStyle(errorStyle);
    }

    private void resetStyle() {
        userField.setStyle("");
        passField.setStyle("");
    }

    private void navigateToRole(String role) {
        SceneManager.showView(role);
    }

    private void openDBConfigPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/DbConfigPopup.fxml")
            );

            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.setTitle("Configurazione Database");
            popupStage.setScene(new Scene(root));
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner(gearIcon.getScene().getWindow());
            popupStage.setResizable(false);

            popupStage.showAndWait();

            boolean ok = ConnectionManager.testConnection();
            AppStatus.setDbConnectionOk(ok);
            updateDbStatusIndicator();

            if (!ok) {
                String error = ConnectionManager.getLastConnectionError();
                logger.log(Level.WARNING, "DB non raggiungibile dopo configurazione: {0}", error);
            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Errore apertura popup configurazione DB", ex);
            boolean ok = ConnectionManager.testConnection();
            AppStatus.setDbConnectionOk(ok);
        }
    }

    private void updateDbStatusIndicator() {
        if (!ConnectionManager.isConfigured()) {
            dbStatusCircle.setFill(Color.GOLD);
            Tooltip.install(dbStatusCircle,
                    new Tooltip("Database non configurato. Clicca l'ingranaggio per configurare."));
            return;
        }

        if (AppStatus.isDbConnectionOk()) {
            dbStatusCircle.setFill(Color.LIMEGREEN);
            Tooltip.install(dbStatusCircle, new Tooltip("Connessione al database attiva"));
        } else {
            dbStatusCircle.setFill(Color.RED);
            String error = ConnectionManager.getLastConnectionError();
            String tooltipText = error != null
                    ? "Connessione fallita: " + error
                    : "Connessione al database fallita";
            Tooltip errorTooltip = new Tooltip(tooltipText);
            errorTooltip.setWrapText(true);
            errorTooltip.setMaxWidth(350);
            Tooltip.install(dbStatusCircle, errorTooltip);
        }
    }

    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(
                    LoginController.class.getResource("/LoginView.fxml")).load();
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile caricare LoginView.fxml", e);
        }
    }
}