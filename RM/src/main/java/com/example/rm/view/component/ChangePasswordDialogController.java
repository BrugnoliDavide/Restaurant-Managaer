package com.example.rm.view.component;

import com.example.rm.app.UserSession;
import com.example.rm.controller.UserAccountController;
import com.example.rm.controller.UserAccountUseCase;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangePasswordDialogController {

    private static final Logger logger = Logger.getLogger(ChangePasswordDialogController.class.getName());
    private static final int MIN_PASSWORD_LENGTH = 6;

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblUsernameLabel;
    @FXML private Label lblUsername;
    @FXML private Label lblRequirements;
    @FXML private Label lblFeedback;

    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private Stage stage;
    private final UserAccountUseCase accountService = new UserAccountController();
    private String username;
    private static String cpfielderrorString = "cp-field-error";
    
    void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        username = UserSession.getInstance().getUser().getUsername();
        lblUsername.setText(username);
        lblFeedback.setVisible(false);

        btnCancel.setOnAction(e -> stage.close());
        btnSave.setOnAction(e -> handleSave());
    }

    private void handleSave() {
        String currentPwd = txtCurrentPassword.getText();
        String newPwd = txtNewPassword.getText();
        String confirmPwd = txtConfirmPassword.getText();

        resetFieldStyles();
        lblFeedback.setVisible(false);

        if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showError("Tutti i campi sono obbligatori");
            highlightEmptyFields();
            return;
        }

        if (newPwd.length() < MIN_PASSWORD_LENGTH) {
            showError("La nuova password deve essere di almeno " + MIN_PASSWORD_LENGTH + " caratteri");
            highlightField(txtNewPassword);
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showError("Le password non coincidono");
            highlightField(txtNewPassword);
            highlightField(txtConfirmPassword);
            return;
        }

        if (currentPwd.equals(newPwd)) {
            showError("La nuova password deve essere diversa da quella corrente");
            highlightField(txtNewPassword);
            return;
        }

        btnSave.setDisable(true);
        btnSave.setText("Salvataggio...");

        boolean success = accountService.changePassword(username, currentPwd, newPwd);

        if (success) {
            showSuccessAlert();
            stage.close();
        } else {
            showError("Password corrente errata. Riprova.");
            highlightField(txtCurrentPassword);
            txtCurrentPassword.clear();
            txtCurrentPassword.requestFocus();
        }

        btnSave.setDisable(false);
        btnSave.setText("Salva");
    }

    private void showError(String message) {
        lblFeedback.setText("⚠ " + message);
        lblFeedback.getStyleClass().removeIf(s -> s.equals("cp-feedback-success"));
        if (!lblFeedback.getStyleClass().contains("cp-feedback-error")) {
            lblFeedback.getStyleClass().add("cp-feedback-error");
        }
        lblFeedback.setVisible(true);
    }

    private void highlightField(PasswordField field) {
        if (!field.getStyleClass().contains(cpfielderrorString)) {
            field.getStyleClass().add(cpfielderrorString);
        }
    }

    private void highlightEmptyFields() {
        if (txtCurrentPassword.getText().isEmpty()) highlightField(txtCurrentPassword);
        if (txtNewPassword.getText().isEmpty()) highlightField(txtNewPassword);
        if (txtConfirmPassword.getText().isEmpty()) highlightField(txtConfirmPassword);
    }

    private void resetFieldStyles() {
        txtCurrentPassword.getStyleClass().remove(cpfielderrorString);
        txtNewPassword.getStyleClass().remove(cpfielderrorString);
        txtConfirmPassword.getStyleClass().remove(cpfielderrorString);
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Cambiata");
        alert.setHeaderText("Operazione completata con successo!");
        alert.setContentText("La password per l'utente \"" + username + "\" è stata aggiornata correttamente.");
        alert.showAndWait();
        logger.log(Level.INFO, "Password cambiata con successo per utente: {0}", username);
    }
}
