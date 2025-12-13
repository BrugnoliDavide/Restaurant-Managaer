package com.example.demo.view;

import com.example.demo.app.UsersFactory;
import com.example.demo.app.UserSession;
import com.example.demo.service.SecurityService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML private TextField userField;
    @FXML private PasswordField passField;

    @FXML
    private void handleLogin() {
        // 1. RESET STILE (Rimuove il rosso precedente se presente)
        resetStyle();

        String user = userField.getText() != null ? userField.getText().trim() : "";
        String pass = passField.getText() != null ? passField.getText().trim() : "";

        // 2. CONTROLLO CAMPI VUOTI
        if (user.isEmpty() || pass.isEmpty()) {
            System.out.println("Campi vuoti!");
            showError(); // Mette i bordi rossi
            return;
        }

        // 3. AUTENTICAZIONE
        String role = SecurityService.authenticate(user, pass);

        if (role != null) {
            // LOGIN SUCCESSO
            System.out.println("Login Successo! Ruolo: " + role);

            // A. Crea l'Utente specifico con la Factory
            com.example.demo.model.User currentUser = UsersFactory.createUser(user, role);

            // B. Inizializza la Sessione
            UserSession.getInstance(currentUser);

            // C. Cambia Pagina
            navigateToRole(role.toLowerCase());

        } else {
            // LOGIN FALLITO
            System.out.println("Credenziali Errate");
            showError(); // Mette i bordi rossi
        }
    }

    // --- METODI PER LA GRAFICA ---

    private void showError() {
        // Applica bordo rosso e raggio curvatura
        String errorStyle = "-fx-border-color: #E02E2E; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;";

        userField.setStyle(errorStyle);
        passField.setStyle(errorStyle);

        // Opzionale: Animazione scossa (shake) potrebbe essere aggiunta qui in futuro
    }

    private void resetStyle() {
        // Rimuove gli stili inline (il rosso), tornando allo stile del CSS originale
        userField.setStyle("");
        passField.setStyle("");
    }

    // --- NAVIGAZIONE ---

    private void navigateToRole(String role) {
        try {
            Parent view = null;

            switch (role) {
                case "manager":
                    view = ManagerController.getFXMLView();
                    break;
                case "cameriere":
                    view = WaiterView.getView(); // O WaiterController.getFXMLView() se l'hai convertito
                    break;
                case "cucina":
                    view = KitchenController.getFXMLView();
                    break;
                default:
                    System.err.println("Ruolo non gestito: " + role);
                    return;
            }

            if (view != null && userField.getScene() != null) {
                userField.getScene().setRoot(view);
            }

        } catch (Exception e) {
            e.printStackTrace();
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