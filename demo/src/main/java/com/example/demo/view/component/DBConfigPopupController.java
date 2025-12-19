package com.example.demo.view.component;

import com.example.demo.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DBConfigPopupController {

    @FXML private TextField addressField;
    @FXML private TextField portField;
    @FXML private TextField dbNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;


    @FXML
    private void initialize() {
        // Precompilazione dei campi con i valori correnti
        addressField.setText(DatabaseService.getDBHost());
        portField.setText(DatabaseService.getDBPort());
        dbNameField.setText(DatabaseService.getDBName());
        usernameField.setText(DatabaseService.getDBUser());
                //passwordField.setText(DatabaseService.getDBPassword());


        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> closePopup());
    }


    private void handleSave() {
        String address = addressField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = usernameField.getText().trim();
        String pass = "inserisci password";//passwordField.getText();

        DatabaseService.setConnectionConfig(
                address,
                port,
                dbName,
                user,
                pass
        );

        closePopup();
    }


    private void closePopup() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
