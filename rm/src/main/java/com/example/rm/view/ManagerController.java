package com.example.rm.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.example.rm.app.UserSession;
import com.example.rm.view.component.ChangePasswordDialog;
import javafx.stage.Stage;
import com.example.rm.app.SceneManager;

import java.util.logging.Level;
import java.util.logging.Logger;


public class ManagerController {

    @FXML    private Label lblHeaderName;
    @FXML    private Label lblHeaderRole;
    @FXML    private Label lblWelcomeTop;
    @FXML    private Label lblWelcomeName;
    @FXML    private StackPane profileBtn;
    @FXML    private Circle profileCircle;

   public static final Logger logger = Logger.getLogger(ManagerController.class.getName());

    @FXML
    public void initialize() {

        UserSession session = UserSession.getInstance();


        String displayName = "Utente";
        String displayRole = "Ruolo";
        String welcomeMsg = "Welcome";

        if (com.example.rm.preference.SimpleGraphicsManager.isEinkMode()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Dashboard non disponibile");
                alert.setHeaderText(null);
                alert.setContentText("Questa dashboard non si può aprire in modalità semplificata.");
                alert.showAndWait();
            });
        }


        if (session != null && session.getUser() != null) {
            // Recuperiamo l'oggetto Utente Polimorfico
            com.example.rm.model.User u = session.getUser();

            displayName = u.getUsername();
            displayRole = u.getRole();

            welcomeMsg = u.getWelcomeMessage();
        }

        lblHeaderName.setText(displayName);
        lblHeaderRole.setText(displayRole);

        // Impostiamo il messaggio personalizzato
        lblWelcomeTop.setText(welcomeMsg);

        Tooltip tooltip = new Tooltip("Opzioni");
        tooltip.setShowDelay(Duration.millis(50));
        Tooltip.install(profileBtn, tooltip);

        // Animazioni Hover
        if (profileCircle != null) {
            profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
            profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));
        }
    }


    @FXML
    private void handleProfileMenu(MouseEvent event) {

        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemStaff = new MenuItem("Gestione Staff");
        itemStaff.getStyleClass().add("context-menu-item");
        itemStaff.setOnAction(
                e ->SceneManager.showUsers());

        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.getStyleClass().add("context-menu-item-info");
        itemChangePassword.setOnAction(e -> {
            Stage stage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(stage);
        });


        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.getStyleClass().add("context-menu-item-danger");
        itemLogout.setOnAction(e -> {
            SceneManager.clearViewCache();
            UserSession.cleanUserSession();
            SceneManager.showLogin();
        });


        contextMenu.getItems().addAll(
                itemStaff,
                new SeparatorMenuItem(),
                itemChangePassword,
                new SeparatorMenuItem(),
                itemLogout
        );

        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }


    @FXML
    private void goToMenu() {
        try {
            SceneManager.showMenu();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore goToMenu: {0}", e.getMessage());
        }
    }

    @FXML
    private void goToFinancial() {
        try {
            SceneManager.showFinancial();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore goToFinancial: {0}", e.getMessage());
       }
    }
}