package com.example.rm.view.component;

import com.example.rm.app.MainApp;
import com.example.rm.controller.MenuService;
import com.example.rm.controller.MenuUseCase;
import com.example.rm.model.MenuProduct;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddProductDialog {

    private static MenuUseCase menuUseCase = new MenuService();

    public static void setMenuUseCase(MenuUseCase useCase) {
        menuUseCase = useCase;
    }



    private AddProductDialog() {
        throw new IllegalStateException("Classe di utilità: non può essere istanziata");
    }

    private static final Logger logger = Logger.getLogger(AddProductDialog.class.getName());

    // Metodi legacy per compatibilità (senza callback)
    public static void display() {
        new DialogBuilder(null, null).show();
    }

    public static void displayEdit(MenuProduct productToEdit, Consumer<Boolean> onComplete) {
        new DialogBuilder(productToEdit, onComplete).show();
    }


    private static class DialogBuilder {
        private final MenuProduct productToEdit;
        private final boolean isEditMode;
        private final Stage window;
        private final Consumer<Boolean> onComplete;

        private TextField txtName;
        private ComboBox<String> cmbType;
        private TextField txtPrice;
        private TextField txtCost;
        private TextField txtAllergens;
        private final MenuUseCase menuService;

        public DialogBuilder(MenuProduct productToEdit, Consumer<Boolean> onComplete) {
            this.productToEdit = productToEdit;
            this.isEditMode = productToEdit != null;
            this.onComplete = onComplete;
            this.menuService = AddProductDialog.menuUseCase;
            this.window = createWindow();
        }
        public void show() {
            VBox layout = buildLayout();
            window.setScene(new Scene(layout));

            // Gestisce la chiusura della finestra
            window.setOnCloseRequest(e -> notifyCompletion(false));

            window.showAndWait();
        }

        private Stage createWindow() {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(isEditMode ? "Modifica Prodotto" : "Aggiungi Prodotto");
            stage.setMinWidth(300);
            MainApp.setLogo(stage);
            return stage;
        }

        private VBox buildLayout() {
            VBox layout = new VBox(10);
            layout.setPadding(new Insets(20));
            layout.setStyle("-fx-background-color: white;");

            initializeFormFields();
            Button btnSave = createSaveButton();
            Button btnCancel = createCancelButton();

            HBox buttonBox = new HBox(10);
            buttonBox.getChildren().addAll(btnCancel, btnSave);

            layout.getChildren().addAll(
                    new Label("Nome:"), txtName,
                    new Label("Tipologia:"), cmbType,
                    new Label("Prezzo (€):"), txtPrice,
                    new Label("Costo (€):"), txtCost,
                    new Label("Allergeni:"), txtAllergens,
                    new Label(""),
                    buttonBox
            );

            return layout;
        }

        private void initializeFormFields() {
            txtName = createTextField(
                    isEditMode ? productToEdit.getNome() : "",
                    "Nome"
            );

            cmbType = createCategoryComboBox();

            txtPrice = createTextField(
                    isEditMode ? String.valueOf(productToEdit.getPrezzoVendita()) : "",
                    "Prezzo Vendita"
            );

            txtCost = createTextField(
                    isEditMode ? String.valueOf(productToEdit.getCostoRealizzazione()) : "",
                    "Costo Realizzazione"
            );

            txtAllergens = createTextField(
                    isEditMode ? productToEdit.getAllergeni() : "",
                    "Allergeni"
            );
        }

        private TextField createTextField(String initialValue, String promptText) {
            TextField textField = new TextField(initialValue);
            textField.setPromptText(promptText);
            return textField;
        }

        private ComboBox<String> createCategoryComboBox() {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll(menuService.loadCategories());
            comboBox.setEditable(true);
            if (isEditMode) comboBox.setValue(productToEdit.getTipologia());
            else if (!comboBox.getItems().isEmpty()) comboBox.getSelectionModel().selectFirst();
            return comboBox;
        }

        private Button createSaveButton() {
            Button button = new Button(isEditMode ? "Salva Modifiche" : "Aggiungi al Menu");
            button.setStyle("-fx-background-color: #2B2B2B; -fx-text-fill: white; -fx-cursor: hand;");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> handleSave());
            return button;
        }

        private Button createCancelButton() {
            Button button = new Button("Annulla");
            button.setStyle("-fx-background-color: #ccc; -fx-text-fill: #333; -fx-cursor: hand;");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> {
                notifyCompletion(false);
                window.close();
            });
            return button;
        }

        private void handleSave() {
            if (!validateInputs()) {
                showError("Compilare tutti i campi obbligatori");
                return;
            }

            try {
                MenuProduct product = buildProductFromForm();
                boolean success = saveProduct(product);

                if (success) {
                    logger.log(Level.INFO, "Prodotto {0} salvato con successo",
                            isEditMode ? "modificato" : "aggiunto");
                    notifyCompletion(true);
                    window.close();
                } else {
                    logger.log(Level.WARNING, "Errore salvataggio DB");
                    showError("Errore durante il salvataggio nel database");
                }
            } catch (NumberFormatException ex) {
                logger.log(Level.WARNING, "Errore formato numeri: {0}", ex.getMessage());
                showError("Formato numerico non valido per prezzo o costo");
            }
        }

        private boolean validateInputs() {
            return !txtName.getText().trim().isEmpty()
                    && cmbType.getValue() != null
                    && !cmbType.getValue().trim().isEmpty()
                    && !txtPrice.getText().trim().isEmpty()
                    && !txtCost.getText().trim().isEmpty();
        }

        private void showError(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(window);
            alert.showAndWait();
        }

        private MenuProduct buildProductFromForm() {
            String nome = txtName.getText().trim();
            String tipo = cmbType.getValue().trim();
            BigDecimal prezzo = parseBigDecimalValue(txtPrice.getText());
            BigDecimal costo = parseBigDecimalValue(txtCost.getText());
            String allergeni = txtAllergens.getText().trim();

            if (isEditMode) {
                return new MenuProduct(productToEdit.getId(), nome, tipo, prezzo, costo, allergeni);
            } else {
                return new MenuProduct(nome, tipo, prezzo, costo, allergeni);
            }
        }

        private double parseDoubleValue(String text) {
            return Double.parseDouble(text.replace(",", "."));
        }

        private boolean saveProduct(MenuProduct product) {
            return isEditMode
                    ? menuService.updateProduct(product)
                    : menuService.addProduct(product);
        }

        private void notifyCompletion(boolean success) {
            if (onComplete != null) {
                onComplete.accept(success);
            }
        }
    }


    private static BigDecimal parseBigDecimalValue(String text) {
        return new BigDecimal(text.replace(",", "."));
    }
}