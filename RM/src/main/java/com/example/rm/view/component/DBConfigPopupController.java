package com.example.rm.view.component;

import com.example.rm.controller.DBConfigController;
import com.example.rm.controller.DBConfigUseCase;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller JavaFX per il popup di configurazione database.
 */
public class DBConfigPopupController {

    @FXML private TextField addressField;
    @FXML private TextField portField;
    @FXML private TextField dbNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label lblPasswordStatus;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final DBConfigUseCase configService = new DBConfigController();

    @FXML
    private void initialize() {
        // Carica configurazione corrente tramite use case
        DBConfigUseCase.DBConfig config = configService.loadConfig();

        addressField.setText(config.host);
        portField.setText(config.port);
        dbNameField.setText(config.dbName);
        usernameField.setText(config.username);

        if (config.hasPassword) {
            lblPasswordStatus.setText("Password già configurata");
            lblPasswordStatus.setStyle("-fx-text-fill: green;");
        } else {
            lblPasswordStatus.setText("Password non impostata");
            lblPasswordStatus.setStyle("-fx-text-fill: orange;");
        }

        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> closePopup());
    }

    private void handleSave() {
        String address = addressField.getText();
        String port = portField.getText();
        String dbName = dbNameField.getText();
        String user = usernameField.getText();
        String newPass = passwordField.getText();

        boolean success = configService.saveConfig(address, port, dbName, user, newPass);

        if (success) {
            closePopup();
        } else {
            lblPasswordStatus.setText("Errore: verifica i campi obbligatori");
            lblPasswordStatus.setStyle("-fx-text-fill: red;");
        }
    }

    private void closePopup() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
