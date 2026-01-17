package com.example.rm.view.component;

import com.example.rm.controller.DBConfigController;
import com.example.rm.controller.DBConfigUseCase;
import com.example.rm.preference.DemoModeManager;
import com.example.rm.preference.SimpleGraphicsManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller per il popup di configurazione database.
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
    @FXML private CheckBox demoModeCheckBox;
    @FXML private CheckBox chkEink;

    private final DBConfigUseCase configService = new DBConfigController();

    @FXML
    private void initialize() {
       DBConfigUseCase.DBConfig config = configService.loadConfig();

        addressField.setText(config.host);
        portField.setText(config.port);
        dbNameField.setText(config.dbName);
        usernameField.setText(config.username);
        demoModeCheckBox.setSelected(DemoModeManager.isDemoMode());
        chkEink.setSelected(SimpleGraphicsManager.isEinkMode());
        chkEink.selectedProperty().addListener((obs, oldV, newV) ->
                SimpleGraphicsManager.setEinkMode(newV)
        );

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
        DemoModeManager.setDemoMode(demoModeCheckBox.isSelected());

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
