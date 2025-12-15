package com.example.demo.view;

import com.example.demo.app.UsersFactory;
import com.example.demo.app.UserSession;
import com.example.demo.service.SecurityService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

public class LoginController {

    @FXML private FontIcon gearIcon;
    @FXML private TextField userField;
    @FXML private PasswordField passField;

    public static final Logger logger = Logger.getLogger(LoginController.class.getName());


    @FXML
    private void handleLogin() {
        // Rimuove il rosso nei campi ca compilare se precedentemente era presente
        resetStyle();

        String user = userField.getText() != null ? userField.getText().trim() : "";
        String pass = passField.getText() != null ? passField.getText().trim() : "";

        logger.log(
                Level.INFO, "Tentativo di autenticazione per l'username: '{0}'", user
        );

        // CONTROLLO CAMPI VUOTI
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
            logger.log(Level.WARNING,"Login FALLITO per credenziali errate per username: '{0}'" , user );
            //questo print viene lasciato per far in modo che se si vedesse la console è evidente l'errpre
            //System.out.println("Credenziali Errate");
            showError(); // Mette i bordi rossi
        }
    }



    private void showError() {
        // Applica bordo rosso e raggio curvatura
        String errorStyle = "-fx-border-color: #E02E2E; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;";

        userField.setStyle(errorStyle);
        passField.setStyle(errorStyle);

        // Opzionale: Animazione scossa (shake) potrebbe essere aggiunta qui in futuro
    }

    private void resetStyle() {
        userField.setStyle("");
        passField.setStyle("");
    }



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

    // Helper statico
    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(LoginController.class.getResource("/LoginView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare LoginView.fxml", e);
        }
    }
}