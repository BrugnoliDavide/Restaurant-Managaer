package com.example.rm.view.component;

import com.example.rm.app.UserSession;
import com.example.rm.service.SecurityService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog per il cambio password sicuro
 * Richiede password corrente, nuova password e conferma
 */
public class ChangePasswordDialog {

    private static final Logger logger =
            Logger.getLogger(ChangePasswordDialog.class.getName());

    private static final int MIN_PASSWORD_LENGTH = 6;

    // Non istanziabile
    private ChangePasswordDialog() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Mostra il dialog per cambiare password
     * @param owner Finestra proprietaria
     */
    public static void show(Stage owner) {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Cambia Password");
        dialog.setResizable(false);

        // === CONTAINER PRINCIPALE ===
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: white;");

        // === TITOLO ===
        Label lblTitle = new Label("Cambio Password");
        lblTitle.setStyle(
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        Label lblSubtitle = new Label(
                "Inserisci la tua password corrente e la nuova password"
        );
        lblSubtitle.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #666; " +
                        "-fx-padding: 0 0 10 0;"
        );

        // === USERNAME DISPLAY (NON MODIFICABILE) ===
        String username = UserSession.getInstance().getUser().getUsername();
        Label lblUsernameLabel = new Label("Utente:");
        lblUsernameLabel.setStyle("-fx-font-weight: bold;");

        Label lblUsername = new Label(username);
        lblUsername.setStyle(
                "-fx-text-fill: #2196F3; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px;"
        );

        VBox userBox = new VBox(5, lblUsernameLabel, lblUsername);
        userBox.setStyle(
                "-fx-background-color: #F5F5F5; " +
                        "-fx-padding: 10; " +
                        "-fx-background-radius: 5;"
        );

        // === CAMPI PASSWORD ===
        VBox fieldsBox = new VBox(15);

        // Password corrente
        Label lblCurrent = new Label("Password Corrente:");
        lblCurrent.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        PasswordField txtCurrentPassword = new PasswordField();
        txtCurrentPassword.setPromptText("Inserisci password attuale");
        txtCurrentPassword.setStyle(
                "-fx-pref-width: 300; " +
                        "-fx-pref-height: 35; " +
                        "-fx-font-size: 13px;"
        );

        // Nuova password
        Label lblNew = new Label("Nuova Password:");
        lblNew.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        PasswordField txtNewPassword = new PasswordField();
        txtNewPassword.setPromptText("Minimo " + MIN_PASSWORD_LENGTH + " caratteri");
        txtNewPassword.setStyle(
                "-fx-pref-width: 300; " +
                        "-fx-pref-height: 35; " +
                        "-fx-font-size: 13px;"
        );

        // Conferma nuova password
        Label lblConfirm = new Label("Conferma Nuova Password:");
        lblConfirm.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        PasswordField txtConfirmPassword = new PasswordField();
        txtConfirmPassword.setPromptText("Reinserisci nuova password");
        txtConfirmPassword.setStyle(
                "-fx-pref-width: 300; " +
                        "-fx-pref-height: 35; " +
                        "-fx-font-size: 13px;"
        );

        fieldsBox.getChildren().addAll(
                lblCurrent, txtCurrentPassword,
                lblNew, txtNewPassword,
                lblConfirm, txtConfirmPassword
        );

        // === LABEL FEEDBACK ===
        Label lblFeedback = new Label();
        lblFeedback.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        lblFeedback.setVisible(false);
        lblFeedback.setWrapText(true);
        lblFeedback.setMaxWidth(300);

        // === REQUISITI PASSWORD ===
        Label lblRequirements = new Label(
                "✓ Minimo " + MIN_PASSWORD_LENGTH + " caratteri\n" +
                        "✓ Usa lettere, numeri e simboli per maggiore sicurezza"
        );
        lblRequirements.setStyle(
                "-fx-text-fill: #666; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-style: italic;"
        );

        // === PULSANTI ===
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button btnCancel = new Button("Annulla");
        btnCancel.setStyle(
                "-fx-background-color: #E0E0E0; " +
                        "-fx-text-fill: #333; " +
                        "-fx-font-weight: bold; " +
                        "-fx-pref-width: 120; " +
                        "-fx-pref-height: 35; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );
        btnCancel.setOnMouseEntered(e ->
                btnCancel.setStyle(btnCancel.getStyle() + "-fx-background-color: #D0D0D0;")
        );
        btnCancel.setOnMouseExited(e ->
                btnCancel.setStyle(btnCancel.getStyle().replace("-fx-background-color: #D0D0D0;", ""))
        );
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("Salva");
        btnSave.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-pref-width: 120; " +
                        "-fx-pref-height: 35; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );
        btnSave.setOnMouseEntered(e ->
                btnSave.setStyle(btnSave.getStyle() + "-fx-background-color: #45a049;")
        );
        btnSave.setOnMouseExited(e ->
                btnSave.setStyle(btnSave.getStyle().replace("-fx-background-color: #45a049;", ""))
        );

        buttonsBox.getChildren().addAll(btnCancel, btnSave);

        // === ASSEMBLAGGIO ===
        root.getChildren().addAll(
                lblTitle,
                lblSubtitle,
                userBox,
                fieldsBox,
                lblRequirements,
                lblFeedback,
                buttonsBox
        );

        // === LOGICA SALVATAGGIO ===
        btnSave.setOnAction(e -> {
            String currentPwd = txtCurrentPassword.getText();
            String newPwd = txtNewPassword.getText();
            String confirmPwd = txtConfirmPassword.getText();

            // Reset stili
            resetFieldStyles(txtCurrentPassword, txtNewPassword, txtConfirmPassword);
            lblFeedback.setVisible(false);

            // === VALIDAZIONI ===

            // 1. Campi vuoti
            if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                showError(lblFeedback, "Tutti i campi sono obbligatori");
                highlightEmptyFields(
                        txtCurrentPassword, txtNewPassword, txtConfirmPassword
                );
                return;
            }

            // 2. Lunghezza minima
            if (newPwd.length() < MIN_PASSWORD_LENGTH) {
                showError(
                        lblFeedback,
                        "La nuova password deve essere di almeno " +
                                MIN_PASSWORD_LENGTH + " caratteri"
                );
                highlightField(txtNewPassword);
                return;
            }

            // 3. Conferma password
            if (!newPwd.equals(confirmPwd)) {
                showError(lblFeedback, "Le password non coincidono");
                highlightField(txtNewPassword);
                highlightField(txtConfirmPassword);
                return;
            }

            // 4. Password uguale alla corrente
            if (currentPwd.equals(newPwd)) {
                showError(
                        lblFeedback,
                        "La nuova password deve essere diversa da quella corrente"
                );
                highlightField(txtNewPassword);
                return;
            }

            // === TENTATIVO CAMBIO PASSWORD ===
            btnSave.setDisable(true);
            btnSave.setText("Salvataggio...");

            boolean success = SecurityService.changePassword(
                    username,
                    currentPwd,
                    newPwd
            );

            if (success) {
                // Successo: mostra conferma e chiudi
                showSuccessAlert(username);
                dialog.close();
            } else {
                // Errore: password corrente errata
                showError(
                        lblFeedback,
                        "Password corrente errata. Riprova."
                );
                highlightField(txtCurrentPassword);
                txtCurrentPassword.clear();
                txtCurrentPassword.requestFocus();

                btnSave.setDisable(false);
                btnSave.setText("Salva");
            }
        });

        // === MOSTRA DIALOG ===
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Mostra messaggio di errore
     */
    private static void showError(Label lblFeedback, String message) {
        lblFeedback.setText("⚠ " + message);
        lblFeedback.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 12px;");
        lblFeedback.setVisible(true);
    }

    /**
     * Evidenzia campo con bordo rosso
     */
    private static void highlightField(PasswordField field) {
        field.setStyle(field.getStyle() +
                "-fx-border-color: #D32F2F; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 3;"
        );
    }

    /**
     * Evidenzia campi vuoti
     */
    private static void highlightEmptyFields(PasswordField... fields) {
        for (PasswordField field : fields) {
            if (field.getText().isEmpty()) {
                highlightField(field);
            }
        }
    }

    /**
     * Reset stili campi
     */
    private static void resetFieldStyles(PasswordField... fields) {
        String normalStyle =
                "-fx-pref-width: 300; " +
                        "-fx-pref-height: 35; " +
                        "-fx-font-size: 13px;";

        for (PasswordField field : fields) {
            field.setStyle(normalStyle);
        }
    }

    /**
     * Mostra alert di successo
     */
    private static void showSuccessAlert(String username) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Cambiata");
        alert.setHeaderText("Operazione completata con successo!");
        alert.setContentText(
                "La password per l'utente '" + username +
                        "' è stata aggiornata correttamente."
        );

        // Stile personalizzato
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        alert.showAndWait();

        logger.log(
                Level.INFO,
                "Password cambiata con successo per utente: {0}",
                username
        );
    }
}
