package com.example.rm.view.component;

import com.example.rm.service.DBConfigStore;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DBConfigPopupController {

    @FXML private TextField addressField;
    @FXML private TextField portField;
    @FXML private TextField dbNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label lblPasswordStatus;


    @FXML private Button saveButton;
    @FXML private Button cancelButton;


    @FXML
    private void initialize() {

        addressField.setText(DatabaseService.getDBHost());
        portField.setText(DatabaseService.getDBPort());
        dbNameField.setText(DatabaseService.getDBName());
        usernameField.setText(DatabaseService.getDBUser());

        if (DatabaseService.hasPassword()) {
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

        String address = addressField.getText().trim();
        String port    = portField.getText().trim();
        String dbName  = dbNameField.getText().trim();
        String user    = usernameField.getText().trim();

        String newPass = passwordField.getText().trim();

        String finalPass = newPass.isBlank()
                ? DBConfigStore.getPassword()
                : newPass;


        DBConfigStore.save(
                address,
                port,
                dbName,
                user,
                finalPass
        );

        DatabaseService.setConnectionConfig(
                address,
                port,
                dbName,
                user,
                finalPass
        );

        closePopup();
    }



    private void closePopup() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
