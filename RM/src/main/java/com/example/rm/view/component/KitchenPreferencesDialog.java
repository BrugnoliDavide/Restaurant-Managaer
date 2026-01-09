package com.example.rm.view.component;

import com.example.rm.controller.KitchenPreferencesUseCase;
import com.example.rm.controller.KitchenPreferencesController;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.example.rm.dao.CategoryDAO;
import com.example.rm.dao.DatabaseCategoryDAO;

public class KitchenPreferencesDialog {
    private static final Logger logger = Logger.getLogger(KitchenPreferencesDialog.class.getName());

    @FXML private CheckBox chkSplitOrders;
    @FXML private ScrollPane scrollCategories;
    @FXML private VBox vboxCategories;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Button btnReset;

    private Stage stage;
    private String username;
    private KitchenPreferences currentPreferences;
    private Consumer<Boolean> onComplete;
    private final KitchenPreferencesUseCase prefsService = new KitchenPreferencesController();
    private final CategoryDAO categoryDAO = new DatabaseCategoryDAO();

    public static void show(Stage owner, String username, Consumer<Boolean> onComplete) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    KitchenPreferencesDialog.class.getResource("/KitchenPreferencesDialog.fxml"));
            VBox root = loader.load();
            KitchenPreferencesDialog controller = loader.getController();
            controller.initialize(username, onComplete);

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
        }
    }

    private void initialize(String username, Consumer<Boolean> onComplete) {
        this.username = username;
        this.onComplete = onComplete;
        this.currentPreferences = prefsService.load(username);
        populateCategories();
        chkSplitOrders.setSelected(currentPreferences.isSplitMixedCategoryOrders());

        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> handleCancel());
        btnReset.setOnAction(e -> handleReset());
    }

    private void populateCategories() {
        vboxCategories.getChildren().clear();

        List<String> allCategories = categoryDAO.getAllCategories();
        Set<String> selectedCategories = currentPreferences.getSelectedCategories();

        for (String category : allCategories) {
            CheckBox chk = new CheckBox(category);
            chk.setStyle("-fx-font-size: 12px");
            chk.setPadding(new Insets(5, 0, 5, 0));
            chk.setSelected(selectedCategories.contains(category));
            vboxCategories.getChildren().add(chk);
        }

        if (allCategories.isEmpty()) {
            Label lblNoCategories = new Label("Nessuna categoria disponibile nel menu");
            lblNoCategories.setStyle("-fx-text-fill: #999; -fx-font-style: italic");
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

            currentPreferences.setSplitMixedCategoryOrders(chkSplitOrders.isSelected());
            currentPreferences.setSelectedCategories(selectedCategories);

            boolean success = prefsService.save(currentPreferences);
            if (success) {
                showSuccessAlert("Preferenze salvate con successo!");
                if (onComplete != null) onComplete.accept(true);
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
        if (onComplete != null) onComplete.accept(false);
        stage.close();
    }

    @FXML
    private void handleReset() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Ripristina Preferenze");
        confirmDialog.setHeaderText("Sei sicuro?");
        confirmDialog.setContentText("Tutte le preferenze verranno ripristinate ai valori di default.");
        confirmDialog.initOwner(stage);

        if (confirmDialog.showAndWait().get() == ButtonType.OK) {
            boolean success = prefsService.reset(username);
            if (success) {
                currentPreferences = prefsService.load(username);
                chkSplitOrders.setSelected(currentPreferences.isSplitMixedCategoryOrders());
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
