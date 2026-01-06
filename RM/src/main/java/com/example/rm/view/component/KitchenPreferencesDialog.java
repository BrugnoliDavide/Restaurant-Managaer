package com.example.rm.view.component;

import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * Dialog per la configurazione delle preferenze della cucina.
 * Permette di:
 * - Attivare/disattivare la scomposizione di ordini multi-categoria
 * - Selezionare le categorie da visualizzare
 * - Attivare l'opzione "Altro" per categorie future
 */
public class KitchenPreferencesDialog {

    private static final Logger logger = Logger.getLogger(KitchenPreferencesDialog.class.getName());

    @FXML
    private CheckBox chkSplitOrders;

    @FXML
    private ScrollPane scrollCategories;

    @FXML
    private VBox vboxCategories;

//    @FXML
//    private CheckBox chkIncludeOther;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnReset;

    private Stage stage;
    private String username;
    private KitchenPreferences currentPreferences;
    private Consumer<Boolean> onComplete;


    public static void show(Stage owner, String username, Consumer<Boolean> onComplete) {
        try {
            // Carica il file FXML
            FXMLLoader loader = new FXMLLoader(
                    KitchenPreferencesDialog.class.getResource("/KitchenPreferencesDialog.fxml")
            );
            VBox root = loader.load();

            // Prendi il controller dal loader
            KitchenPreferencesDialog controller = loader.getController();
            controller.initialize(username, onComplete);

            // Crea il dialog
            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(owner);
            dialog.setTitle("Preferenze Cucina");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.setWidth(500);
            dialog.setHeight(600);

            controller.stage = dialog;
            dialog.showAndWait();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento KitchenPreferencesDialog.fxml", e);
            e.printStackTrace();
        }
    }


    private void initialize(String username, Consumer<Boolean> onComplete) {
        this.username = username;
        this.onComplete = onComplete;


        currentPreferences = DatabaseService.getKitchenPreferences(username);

        // Popola CheckBox per categorie
        populateCategories();

        // Setta valori UI dalle preferenze caricate
        chkSplitOrders.setSelected(currentPreferences.isSplitMixedCategoryOrders());
        //chkIncludeOther.setSelected(currentPreferences.isIncludeOtherCategories());

        // Listeners pulsanti
        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> handleCancel());
        btnReset.setOnAction(e -> handleReset());
    }

    /**
     * Popola la VBox con CheckBox per ogni categoria disponibile nel menu.
     */
    private void populateCategories() {
        vboxCategories.getChildren().clear();

        // Recupera categorie dal DB
        List<String> allCategories = DatabaseService.getAllCategories();
        Set<String> selectedCategories = currentPreferences.getSelectedCategories();

        for (String category : allCategories) {
            CheckBox chk = new CheckBox(category);
            chk.setStyle("-fx-font-size: 12px;");
            chk.setPadding(new Insets(5, 0, 5, 0));
            chk.setSelected(selectedCategories.contains(category));

            vboxCategories.getChildren().add(chk);
        }

        // Se nessuna categoria nel DB, mostra messaggio
        if (allCategories.isEmpty()) {
            Label lblNoCategories = new Label("Nessuna categoria disponibile nel menu");
            lblNoCategories.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            lblNoCategories.setPadding(new Insets(10));
            vboxCategories.getChildren().add(lblNoCategories);
        }
    }


    @FXML
    private void handleSave() {
        try {
            // Raccogli categorie selezionate
            Set<String> selectedCategories = new HashSet<>();
            for (var node : vboxCategories.getChildren()) {
                if (node instanceof CheckBox) {
                    CheckBox chk = (CheckBox) node;
                    if (chk.isSelected()) {
                        selectedCategories.add(chk.getText());
                    }
                }
            }

            // Aggiorna l'oggetto preferenze
            currentPreferences.setSplitMixedCategoryOrders(chkSplitOrders.isSelected());
            currentPreferences.setSelectedCategories(selectedCategories);
            //currentPreferences.setIncludeOtherCategories(chkIncludeOther.isSelected());

            // Salva nel DB
            boolean success = DatabaseService.saveKitchenPreferences(currentPreferences);

            if (success) {
                showSuccessAlert("Preferenze salvate con successo!");
                if (onComplete != null) {
                    onComplete.accept(true);  // ← Questo chiama il callback
                }
                stage.close();
            } else {
                showErrorAlert("Errore durante il salvataggio delle preferenze");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore salvataggio preferenze cucina", e);
            showErrorAlert("Errore: " + e.getMessage());
        }
    }


    @FXML
    private void handleCancel() {
        if (onComplete != null) {
            onComplete.accept(false);
        }
        stage.close();
    }

    /**
     * Gestisce il click su "Ripristina Preferenze di Default".
     */
    @FXML
    private void handleReset() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Ripristina Preferenze");
        confirmDialog.setHeaderText("Sei sicuro?");
        confirmDialog.setContentText("Tutte le preferenze verranno ripristinate ai valori di default.");
        confirmDialog.initOwner(stage);

        if (confirmDialog.showAndWait().get() == ButtonType.OK) {
            // Resetta a default
            boolean success = DatabaseService.deleteKitchenPreferences(username);

            if (success) {
                currentPreferences = DatabaseService.getKitchenPreferences(username);

                // Aggiorna UI
                chkSplitOrders.setSelected(currentPreferences.isSplitMixedCategoryOrders());
                //chkIncludeOther.setSelected(currentPreferences.isIncludeOtherCategories());
                populateCategories();

                showSuccessAlert("Preferenze ripristinate ai valori di default");
            } else {
                showErrorAlert("Errore durante il reset delle preferenze");
            }
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
