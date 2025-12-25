package com.example.rm.view;

import com.example.rm.app.AppStatus;
import com.example.rm.app.UsersFactory;
import com.example.rm.app.UserSession;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.SecurityService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;


public class LoginController {

    @FXML private FontIcon gearIcon;
    @FXML private TextField userField;
    @FXML private PasswordField passField;

    @FXML private Circle dbStatusCircle;


    //private javafx.stage.Popup dbConfigPopup;


    public static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML
    public void initialize() {

        updateDbStatusIndicator();


        if (gearIcon == null) {
            throw new IllegalStateException("gearIcon non è stato iniettato correttamente dal file FXML");
        }
        gearIcon.setOnMouseClicked(event -> openDBConfigPopup());
    }

    @FXML
    private void handleLogin() {
        // Rimuove il rosso nei campi ca compilare se precedentemente era presente
        resetStyle();

        boolean ok = DatabaseService.testConnection();
        AppStatus.setDbConnectionOk(ok);
        updateDbStatusIndicator();

        if (!ok) {
            logger.warning("Login bloccato: DB non raggiungibile");
            return;
        }



        String user = userField.getText() != null ? userField.getText().trim() : "";
        String pass = passField.getText() != null ? passField.getText().trim() : "";

        logger.log(
                Level.INFO, "Tentativo di autenticazione per  username: {0} ", user
        );


        if (user.isEmpty() || pass.isEmpty()) {
            //logger.warning("campi vuoti");
            showError(); // Mette i bordi rossi
            return;
        }


        String role = SecurityService.authenticate(user, pass);

        if (role != null) {

            logger.log(
                    Level.INFO,"Login COMPLETATO per utente: {0} [Ruolo assegnato: {1}]",
                    new Object[]{ user, role }
            );

            //Crea l'Utente specifico con la Factory
            com.example.rm.model.User currentUser = UsersFactory.createUser(user, role);


            UserSession.getInstance(currentUser);


            navigateToRole(role.toLowerCase());

        } else {
            showError(); // Mette i bordi rossi
        }
    }


    private void showError() {
        // bordo rosso
        String errorStyle = "-fx-border-color: #E02E2E; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;";

        userField.setStyle(errorStyle);
        passField.setStyle(errorStyle);

        //  Animazione scossa (shake) aggiungere qui in futuro
    }

    private void resetStyle() {
        userField.setStyle("");
        passField.setStyle("");
    }

    private void navigateToRole(String role) {

        View view = ViewFactory.forRole(role);

        userField
                .getScene()
                .setRoot(view.getRoot());
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

            boolean ok = DatabaseService.testConnection();
            AppStatus.setDbConnectionOk(ok);
            updateDbStatusIndicator();

            if (!ok) {
                logger.warning("Login bloccato: DB non raggiungibile");
            }



        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Errore apertura popup configurazione DB", ex);

            boolean ok = DatabaseService.testConnection();
            AppStatus.setDbConnectionOk(ok);

        }
    }

    private void updateDbStatusIndicator() {

        if (!DatabaseService.isConfigured()) {
            dbStatusCircle.setFill(Color.GOLD);   // giallo → non configurato
            return;
        }

        if (AppStatus.isDbConnectionOk()) {
            dbStatusCircle.setFill(Color.LIMEGREEN); // verde
        } else {
            dbStatusCircle.setFill(Color.RED);       // rosso
        }
    }





    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(LoginController.class.getResource("/LoginView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare LoginView.fxml", e);
        }
    }
}