package com.example.demo.view;

import com.example.demo.service.SecurityService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML private TextField userField;
    @FXML private PasswordField passField; // Assicurati che nel FXML ci sia fx:id="passField"

    @FXML
    private void handleLogin() {
        // 1. Reset Stile (togli il rosso se c'era)
        userField.setStyle("");
        passField.setStyle("");

        // 2. Controllo Input Vuoti
        if (userField.getText() == null || passField.getText() == null) {
            return;
        }

        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            setErrorStyle();
            System.out.println("Inserisci username e password.");
            return;
        }

        // 3. AUTENTICAZIONE SICURA (BCrypt)
        // Chiediamo al SecurityService di verificare l'hash
        String role = SecurityService.authenticate(user, pass);

        if (role != null) {
            System.out.println("Login Successo! Benvenuto " + role);
            navigateToRole(role.toLowerCase());
        } else {
            // Login Fallito
            System.out.println("Credenziali Errate (Username o Password non validi)");
            setErrorStyle();
        }
    }

    // Metodo separato per gestire il cambio pagina
    private void navigateToRole(String role) {
        try {
            Parent view = null;

            switch (role) {
                case "manager":
                    // Carica FXML del Manager
                    view = ManagerController.getFXMLView();
                    break;

                case "cameriere":
                    // Carica Vista Java del Cameriere (se è ancora Java puro)
                    view = WaiterView.getView();
                    break;

                case "cucina":
                    // Carica FXML della Cucina
                    view = KitchenController.getFXMLView();
                    break;

                default:
                    System.err.println("Ruolo '" + role + "' non riconosciuto o non gestito.");
                    setErrorStyle();
                    return;
            }

            // Esegue il cambio scena effettivo
            if (view != null && userField.getScene() != null) {
                userField.getScene().setRoot(view);
            }

        } catch (Exception e) {
            System.err.println("ERRORE CRITICO DURANTE LA NAVIGAZIONE:");
            e.printStackTrace();
        }
    }

    private void setErrorStyle() {
        // Bordo rosso per indicare errore
        String errorStyle = "-fx-border-color: #E02E2E; -fx-border-width: 2px; -fx-border-radius: 6px;";
        userField.setStyle(errorStyle);
        passField.setStyle(errorStyle);
    }

    // Helper statico per caricare la vista Login stessa (usato dal Logout)
    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(LoginController.class.getResource("/LoginView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare LoginView.fxml", e);
        }
    }
}