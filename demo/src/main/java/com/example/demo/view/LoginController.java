package com.example.demo.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML private TextField userField;
    // PasswordField non serve mapparlo se non lo leggiamo, ma per completezza lo lasciamo
    // @FXML private PasswordField passField;

    @FXML
    private void handleLogin() {
        String role = userField.getText().trim().toLowerCase();

        // Logica di navigazione semplice
        switch (role) {
            case "manager":
                System.out.println("DEBUG: Case 'manager' rilevato. Tento il caricamento...");


                Parent view = ManagerController.getFXMLView();

                System.out.println("DEBUG: Vista Manager caricata con successo: " + view);

                if (userField.getScene() == null) {
                    System.err.println("ERRORE: La scena è null. Impossibile navigare.");
                } else {
                    userField.getScene().setRoot(view);
                    System.out.println("DEBUG: Navigazione eseguita.");
                }
                break;
            case "cameriere":
                System.out.println("Login: CAMERIERE");
                userField.getScene().setRoot(WaiterView.getView());
                break;
            case "cucina":
                System.out.println("Login: cucina");
                userField.getScene().setRoot(KitchenController.getFXMLView());
                break;

            default:
                // Errore visivo
                userField.setStyle("-fx-border-color: red; -fx-border-radius: 8px;");
        }
    }

    // Metodo helper statico per caricare la vista
    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(LoginController.class.getResource("/LoginView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare LoginView.fxml", e);
        }
    }
}