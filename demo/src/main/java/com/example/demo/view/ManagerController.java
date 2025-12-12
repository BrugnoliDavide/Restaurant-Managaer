package com.example.demo.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
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

    private String userName = "Sig.na Susan";
    private String userRole = "Manager";

    @FXML
    public void initialize() {
        if (profileBtn == null) return;

        // Impostazione testi
        lblHeaderName.setText(userName);
        lblHeaderRole.setText(userRole);
        lblWelcomeTop.setText("welcome, " + userName.toLowerCase());

        if (userName.contains(" ")) lblWelcomeName.setText(userName.split(" ")[1]);
        else lblWelcomeName.setText(userName);

        // --- TOOLTIP AGGIORNATO ---
        // Prima era "Logout", ora è "Opzioni Profilo"
        Tooltip tooltip = new Tooltip("Opzioni Profilo");
        tooltip.setShowDelay(Duration.millis(50));
        Tooltip.install(profileBtn, tooltip);

        // Animazioni Hover
        if (profileCircle != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }
    }

    // --- NUOVO METODO: MENU A TENDINA ---
    @FXML
    private void handleProfileMenu(MouseEvent event) {
        // 1. Creiamo il Menu
        ContextMenu contextMenu = new ContextMenu();

        // 2. Voce "Gestione Staff"
        MenuItem itemStaff = new MenuItem("Gestione Staff");
        itemStaff.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10;");
        itemStaff.setOnAction(e -> {
            System.out.println("Navigazione -> Gestione Staff");
            // Carica la vista UsersController che abbiamo creato prima
            profileBtn.getScene().setRoot(UsersController.getFXMLView());
        });

        // 3. Voce "Logout"
        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10; -fx-text-fill: red;"); // Rosso per indicare uscita
        itemLogout.setOnAction(e -> {
            System.out.println("Eseguo Logout...");
            profileBtn.getScene().setRoot(LoginController.getFXMLView());
        });

        // 4. Aggiungiamo le voci al menu (con un separatore in mezzo)
        contextMenu.getItems().addAll(itemStaff, new SeparatorMenuItem(), itemLogout);

        // 5. Mostra il menu SOTTO il bottone del profilo
        // Side.BOTTOM dice "attaccati al lato inferiore del profileBtn"
        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }

    // --- ALTRE NAVIGAZIONI ---

    @FXML
    private void goToMenu() {
        System.out.println("Navigazione -> Menu");
        profileBtn.getScene().setRoot(MenuView.getView());
    }

    @FXML
    private void goToFinancial() {
        System.out.println("Navigazione -> Financial");
        profileBtn.getScene().setRoot(FinancialController.getFXMLView());
    }

    @FXML
    private void handleNotifications() {
        System.out.println("Click Notifiche");
    }

    public static Parent getFXMLView() {
        try {
            return new FXMLLoader(ManagerController.class.getResource("/ManagerView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}