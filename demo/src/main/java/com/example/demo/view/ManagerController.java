package com.example.demo.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;

public class ManagerController {

    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeTop;
    @FXML private Label lblWelcomeName;
    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;

    // Dati Simulati
    private String userName = "Sig.na Susan";
    private String userRole = "Manager";

    @FXML
    public void initialize() {
        // --- 1. DEBUG DI SICUREZZA ---
        if (profileBtn == null) {
            System.err.println("ERRORE GRAVE: 'profileBtn' è NULL. Controlla fx:id nel FXML!");
            return; // Fermiamoci per evitare il crash
        }

        // --- 2. IMPOSTAZIONE TESTI ---
        lblHeaderName.setText(userName);
        lblHeaderRole.setText(userRole);
        lblWelcomeTop.setText("welcome, " + userName.toLowerCase());

        // Gestione sicura del nome (evita crash se non ci sono spazi)
        if (userName.contains(" ")) {
            lblWelcomeName.setText(userName.split(" ")[1]);
        } else {
            lblWelcomeName.setText(userName);
        }

        // --- 3. TOOLTIP (Logout) ---
        // Lo installiamo via codice, così non impazziamo col FXML
        Tooltip tooltip = new Tooltip("Logout");
        tooltip.setShowDelay(Duration.millis(50));
        Tooltip.install(profileBtn, tooltip);

        // --- 4. ANIMAZIONI HOVER ---
        if (profileCircle != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }
    }

    // --- NAVIGAZIONE ---

    @FXML
    private void handleLogout() {
        System.out.println("Logout Manager...");
        profileBtn.getScene().setRoot(LoginController.getFXMLView());
    }

    @FXML
    private void goToMenu() {
        System.out.println("Navigazione -> Menu");
        profileBtn.getScene().setRoot(MenuView.getView());
    }

    @FXML
    private void goToFinancial() {
        System.out.println("Navigazione -> Financial (WIP)");
    }

    @FXML
    private void handleNotifications() {
        System.out.println("Navigazione -> Financial");
        profileBtn.getScene().setRoot(FinancialController.getFXMLView());
    }

    // --- HELPER CARICAMENTO ---
    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(ManagerController.class.getResource("/ManagerView.fxml")).load();
        } catch (IOException e) {
            System.err.println("ERRORE CARICAMENTO FXML MANAGER:");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}