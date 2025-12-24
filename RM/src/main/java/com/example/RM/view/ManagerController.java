package com.example.RM.view;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.example.RM.app.UserSession;

public class ManagerController {

    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;
    @FXML private Label lblWelcomeTop;
    @FXML private Label lblWelcomeName;
    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;



    @FXML
    public void initialize() {

            UserSession session = UserSession.getInstance();


            String displayName = "Utente";
            String displayRole = "Ruolo";
            String welcomeMsg = "Welcome";

            if (session != null && session.getUser() != null) {
                // Recuperiamo l'oggetto Utente Polimorfico
                com.example.RM.model.User u = session.getUser();

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

        ContextMenu contextMenu = new ContextMenu();


        MenuItem itemStaff = new MenuItem("Gestione Staff");
        itemStaff.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10;");
        itemStaff.setOnAction(e -> {

            View usersView = ViewFactory.forRole("users");
            profileBtn.getScene().setRoot(usersView.getRoot());


            // !! deprecato profileBtn.getScene().setRoot(UsersController.getFXMLView());
        });


        MenuItem itemLogout = new MenuItem("Logout");
        itemLogout.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10; -fx-text-fill: red;"); // Rosso per indicare uscita
        itemLogout.setOnAction(e -> {



            UserSession.cleanUserSession();
            profileBtn.getScene().setRoot(LoginController.getFXMLView());
        });


        contextMenu.getItems().addAll(itemStaff, new SeparatorMenuItem(), itemLogout);


        contextMenu.show(profileBtn, Side.BOTTOM, 0, 0);
    }



    @FXML
    private void goToMenu() {
        // !! togliere import inutilizzati
        //System.out.println("Navigazione -> Menu");
        //profileBtn.getScene().setRoot(MenuView.getView());
        View MenuView = ViewFactory.forRole("menu");
        profileBtn
                .getScene()
                .setRoot(MenuView.getRoot());
    }

    @FXML
    private void goToFinancial() {

        View financialView = ViewFactory.forRole("financial");

        profileBtn
                .getScene()
                .setRoot(financialView.getRoot());
    }

    @FXML
    private void handleNotifications() {
        //System.out.println("Click Notifiche");
    }

    /*public static Parent getFXMLView() {
        try {
            return new FXMLLoader(ManagerController.class.getResource("/ManagerView.fxml")).load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/
}