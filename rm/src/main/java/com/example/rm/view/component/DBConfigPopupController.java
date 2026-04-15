package com.example.rm.view.component;

import com.example.rm.controller.DBConfigController;
import com.example.rm.controller.DBConfigUseCase;
import com.example.rm.controller.DBConfigUseCase.SaveResult;
import com.example.rm.preference.DemoModeManager;
import com.example.rm.preference.SimpleGraphicsManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller per il popup di configurazione database.
 * Supporta due modalità:
 * <ul>
 *   <li><strong>Parametri separati:</strong> host, porta, nome DB, username, password</li>
 *   <li><strong>URL diretto:</strong> URL JDBC completo + username + password</li>
 * </ul>
 */
public class DBConfigPopupController {

    private static final Logger logger = Logger.getLogger(DBConfigPopupController.class.getName());

    // --- Campi modalità parametri separati ---
    @FXML private TextField addressField;
    @FXML private TextField portField;
    @FXML private TextField dbNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // --- Campi modalità URL diretto ---
    @FXML private TextArea urlField;
    @FXML private TextField urlUsernameField;
    @FXML private PasswordField urlPasswordField;

    // --- Contenitori per le due modalità ---
    @FXML private VBox paramFieldsBox;
    @FXML private VBox urlFieldsBox;

    // --- Toggle modalità ---
    @FXML private ToggleGroup modeToggle;
    @FXML private RadioButton radioParams;
    @FXML private RadioButton radioUrl;

    // --- Feedback e azioni ---
    @FXML private Label lblStatus;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button testButton;
    @FXML private CheckBox demoModeCheckBox;
    @FXML private CheckBox chkEink;

    private final DBConfigUseCase configService = new DBConfigController();

    @FXML
    private void initialize() {
        DBConfigUseCase.DBConfig config = configService.loadConfig();

        // Popola campi parametri separati
        addressField.setText(config.host);
        portField.setText(config.port);
        dbNameField.setText(config.dbName);
        usernameField.setText(config.username);

        // Popola campo URL diretto
        if (config.jdbcUrl != null && !config.jdbcUrl.isBlank()) {
            urlField.setText(config.jdbcUrl);
        }
        urlUsernameField.setText(config.username);

        // Stato password
        updatePasswordStatus(config.hasPassword);

        // Opzioni
        demoModeCheckBox.setSelected(DemoModeManager.isDemoMode());
        chkEink.setSelected(SimpleGraphicsManager.isEinkMode());
        chkEink.selectedProperty().addListener((obs, oldV, newV) ->
                SimpleGraphicsManager.setEinkMode(newV)
        );

        // Imposta modalità iniziale: parametri separati
        radioParams.setSelected(true);
        showParamsMode();

        // Listener per cambio modalità
        modeToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == radioUrl) {
                showUrlMode();
                // Sincronizza username tra le due modalità
                if (urlUsernameField.getText().isBlank()) {
                    urlUsernameField.setText(usernameField.getText());
                }
            } else {
                showParamsMode();
                if (usernameField.getText().isBlank()) {
                    usernameField.setText(urlUsernameField.getText());
                }
            }
        });

        // Azioni pulsanti
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> closePopup());
        testButton.setOnAction(e -> handleTest());
    }

    /**
     * Mostra la modalità parametri separati.
     */
    private void showParamsMode() {
        paramFieldsBox.setVisible(true);
        paramFieldsBox.setManaged(true);
        urlFieldsBox.setVisible(false);
        urlFieldsBox.setManaged(false);
    }

    /**
     * Mostra la modalità URL diretto.
     */
    private void showUrlMode() {
        paramFieldsBox.setVisible(false);
        paramFieldsBox.setManaged(false);
        urlFieldsBox.setVisible(true);
        urlFieldsBox.setManaged(true);
    }

    /**
     * Salva la configurazione nella modalità attualmente selezionata.
     */
    private void handleSave() {
        DemoModeManager.setDemoMode(demoModeCheckBox.isSelected());
        lblStatus.setVisible(false);

        SaveResult result;

        if (radioUrl.isSelected()) {
            result = configService.saveConfigWithUrl(
                    urlField.getText(),
                    urlUsernameField.getText(),
                    urlPasswordField.getText()
            );
        } else {
            result = configService.saveConfig(
                    addressField.getText(),
                    portField.getText(),
                    dbNameField.getText(),
                    usernameField.getText(),
                    passwordField.getText()
            );
        }

        handleSaveResult(result);
    }

    /**
     * Testa la connessione senza salvare.
     */
    private void handleTest() {
        lblStatus.setVisible(false);

        // Salva temporaneamente per testare
        SaveResult result;

        if (radioUrl.isSelected()) {
            result = configService.saveConfigWithUrl(
                    urlField.getText(),
                    urlUsernameField.getText(),
                    urlPasswordField.getText()
            );
        } else {
            result = configService.saveConfig(
                    addressField.getText(),
                    portField.getText(),
                    dbNameField.getText(),
                    usernameField.getText(),
                    passwordField.getText()
            );
        }

        if (!result.saved) {
            showStatus(result.errorMessage, StatusType.ERROR);
        } else if (result.connectionOk) {
            showStatus("Connessione riuscita!", StatusType.SUCCESS);
        } else {
            showStatus("Connessione fallita:\n" + result.errorMessage, StatusType.ERROR);
        }
    }

    /**
     * Gestisce il risultato dell'operazione di salvataggio.
     */
    private void handleSaveResult(SaveResult result) {
        if (!result.saved) {
            // Errore di validazione
            showStatus(result.errorMessage, StatusType.ERROR);
            return;
        }

        if (result.connectionOk) {
            showStatus("Configurazione salvata. Connessione OK.", StatusType.SUCCESS);
            // Chiudi dopo un breve ritardo per mostrare il messaggio
            closePopup();
        } else {
            // Salvato ma connessione fallita: mostra errore ma non chiudere
            showStatus("Configurazione salvata, ma la connessione è fallita:\n"
                    + result.errorMessage, StatusType.WARNING);
            logger.log(Level.WARNING, "Configurazione salvata ma connessione fallita: {0}",
                    result.errorMessage);
        }
    }

    /**
     * Aggiorna l'indicatore dello stato della password.
     */
    private void updatePasswordStatus(boolean hasPassword) {
        if (hasPassword) {
            showStatus("Password già configurata", StatusType.INFO);
        }
    }

    /**
     * Mostra un messaggio di stato con lo stile appropriato.
     */
    private void showStatus(String message, StatusType type) {
        lblStatus.setText(message);
        lblStatus.setWrapText(true);
        lblStatus.setVisible(true);

        switch (type) {
            case SUCCESS -> lblStatus.setStyle(
                    "-fx-text-fill: #2E7D32; -fx-font-size: 12px; -fx-font-weight: bold;");
            case WARNING -> lblStatus.setStyle(
                    "-fx-text-fill: #E65100; -fx-font-size: 12px;");
            case ERROR -> lblStatus.setStyle(
                    "-fx-text-fill: #C62828; -fx-font-size: 12px; -fx-font-weight: bold;");
            case INFO -> lblStatus.setStyle(
                    "-fx-text-fill: #1565C0; -fx-font-size: 11px;");
        }
    }

    private void closePopup() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private enum StatusType {
        SUCCESS, WARNING, ERROR, INFO
    }
}