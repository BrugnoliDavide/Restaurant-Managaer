package com.example.demo.view;

import com.example.demo.app.AppStatus;
import com.example.demo.app.UsersFactory;
import com.example.demo.app.UserSession;
import com.example.demo.service.DatabaseService;
import com.example.demo.service.SecurityService;
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


    /* !! eliminare se tutti i test vanno a buon fine
    @FXML
    private void handleLoginTest() {
        Parent root = new com.example.demo.view.screens.ManagerView().getRoot();
        userField.getScene().setRoot(root);
    } */



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
            com.example.demo.model.User currentUser = UsersFactory.createUser(user, role);


            UserSession.getInstance(currentUser);


            navigateToRole(role.toLowerCase());

        } else {
            logger.log(Level.WARNING,"Login FALLITO per credenziali errate per username: {0}" , user );
            //questo print viene lasciato per far in modo che se si vedesse la console è evidente l'errOre
            //System.out.println("Credenziali Errate");
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





/* se i test vanno a buon fine eliminare tutto
    private void navigateToRole(String role) {
        try {


            Parent view = null;

            switch (role) {
                case "manager":
                    view = ManagerController.getFXMLView();
                    break;
                case "cameriere":
                    view = WaiterController.getFXMLView();
                    break;
                case "cucina":
                    view = KitchenController.getFXMLView();
                    break;
                default:

                    logger.log(Level.WARNING,"Role non valido");
                    return;
            }

            if (view != null && userField.getScene() != null) {
                userField.getScene().setRoot(view);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore", e);
        }
    }

    /*private void openDBConfigPopup() {

        if (dbConfigPopup != null && dbConfigPopup.isShowing()) {
            dbConfigPopup.hide();
            return;
        }

        if (dbConfigPopup == null) {
            dbConfigPopup = new javafx.stage.Popup();

            javafx.scene.control.Label label =
                    new javafx.scene.control.Label("Configurazione DB");

            javafx.scene.layout.VBox content =
                    new javafx.scene.layout.VBox(label);

            content.getStyleClass().add("login-pop-up-setup-connection");


            dbConfigPopup.getContent().add(content);
        }

        var bounds = gearIcon.localToScreen(gearIcon.getBoundsInLocal());

        dbConfigPopup.show(
                gearIcon,
                bounds.getMinX(),
                bounds.getMaxY()
        );
    }*/


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
                return;
            }



        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Errore apertura popup configurazione DB", ex);

            boolean ok = DatabaseService.testConnection();
            AppStatus.setDbConnectionOk(ok);

        }
    }

    /*
    //metodi per gestire il semaforo di connessione al DB
    private void setDbStatusPending() {
        dbStatusIndicator.setFill(Color.web("#FBC02D")); // Giallo
    }

    private void setDbStatusOk() {
        dbStatusIndicator.setFill(Color.web("#2E7D32")); // Verde
    }

    private void setDbStatusError() {
        dbStatusIndicator.setFill(Color.web("#C62828")); // Rosso
    }*/



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