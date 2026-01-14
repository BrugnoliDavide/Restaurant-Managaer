package com.example.rm.view;

import com.example.rm.controller.ManagerService;
import com.example.rm.controller.ManagerUseCase;
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


public class ManagerController {

    @FXML
    private Label lblHeaderName;
    @FXML
    private Label lblHeaderRole;
    @FXML
    private Label lblWelcomeTop;
    @FXML
    private Label lblWelcomeName;
    @FXML
    private StackPane profileBtn;
    @FXML
    private Circle profileCircle;

    private static ManagerUseCase managerUseCase = new ManagerService();

    public static void setManagerUseCase(ManagerUseCase useCase) {
        managerUseCase = useCase;
    }


    @FXML
    public void initialize() {

        UserSession session = UserSession.getInstance();


        String displayName = "Utente";
        String displayRole = "Ruolo";
        String welcomeMsg = "Welcome";

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

    // MENU A TENDINA
    @FXML
    private void handleProfileMenu(MouseEvent event) {

        if (profileBtn == null || profileBtn.getScene() == null || profileBtn.getScene().getWindow() == null) {
            System.err.println("Contesto finestra non disponibile per ContextMenu");
            return;
        }


        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemStaff = new MenuItem("Gestione Staff");
        itemStaff.getStyleClass().add("context-menu-item");
        //itemStaff.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10;");
        itemStaff.setOnAction(e -> {
            SceneManager.showUsers();
        });

        MenuItem itemChangePassword = new MenuItem("Cambia Password");
        itemChangePassword.getStyleClass().add("context-menu-item-info");
        /*itemChangePassword.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: #2196F3;"
        );*/
        itemChangePassword.setOnAction(e -> {
            Stage stage = (Stage) profileBtn.getScene().getWindow();
            ChangePasswordDialog.show(stage);
        });

        //  LOGOUT
        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.getStyleClass().add("context-menu-item-danger");
        /*itemLogout.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-text-fill: red; " +
                        "-fx-font-weight: bold;"
        );*/
        itemLogout.setOnAction(e -> {
            SceneManager.clearViewCache();
            UserSession.cleanUserSession();
            SceneManager.showLogin();
        });

        // === ASSEMBLA MENU ===
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
            System.err.println("Errore goToMenu: " + e.getMessage());
            e.printStackTrace();  // Forza console
        }
    }

    @FXML
    private void goToFinancial() {
        try {
            SceneManager.showFinancial();
        } catch (Exception e) {
            System.err.println("Errore goToFinancial: " + e.getMessage());
            e.printStackTrace();
        }
    }
}