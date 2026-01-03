package com.example.rm.view.component;

import com.example.rm.app.MainApp;
import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import com.example.rm.view.MenuController;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AddProductDialog {

    private AddProductDialog() {
        throw new IllegalStateException("Classe di utilità: non può essere istanziata");
    }

    private static final Logger logger = Logger.getLogger(MenuController.class.getName());

    public static void display() {
        new DialogBuilder(null).show();
    }

    public static void displayEdit(MenuProduct productToEdit) {
        new DialogBuilder(productToEdit).show();
    }

    /**
     * Classe interna per gestire la creazione e la logica del dialog
     */
    private static class DialogBuilder {
        private final MenuProduct productToEdit;
        private final boolean isEditMode;
        private final Stage window;

        private TextField txtName;
        private ComboBox<String> cmbType;
        private TextField txtPrice;
        private TextField txtCost;
        private TextField txtAllergens;

        public DialogBuilder(MenuProduct productToEdit) {
            this.productToEdit = productToEdit;
            this.isEditMode = (productToEdit != null);
            this.window = createWindow();
        }

        public void show() {
            VBox layout = buildLayout();
            window.setScene(new Scene(layout));
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

            layout.getChildren().addAll(
                    new Label("Nome:"), txtName,
                    new Label("Tipologia:"), cmbType,
                    new Label("Prezzo (€):"), txtPrice,
                    new Label("Costo (€):"), txtCost,
                    new Label("Allergeni:"), txtAllergens,
                    new Label(""), btnSave
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
            comboBox.getItems().addAll(DatabaseService.getAllCategories());
            comboBox.setEditable(true);

            if (isEditMode) {
                comboBox.setValue(productToEdit.getTipologia());
            } else if (!comboBox.getItems().isEmpty()) {
                comboBox.getSelectionModel().selectFirst();
            }

            return comboBox;
        }

        private Button createSaveButton() {
            Button button = new Button(isEditMode ? "Salva Modifiche" : "Aggiungi al Menu");
            button.setStyle("-fx-background-color: #2B2B2B; -fx-text-fill: white; -fx-cursor: hand;");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> handleSave());
            return button;
        }

        private void handleSave() {
            try {
                MenuProduct product = buildProductFromForm();
                boolean success = saveProduct(product);

                if (success) {
                    window.close();
                } else {
                    logger.log(Level.WARNING, "Errore salvataggio DB");
                }
            } catch (NumberFormatException ex) {
                logger.log(Level.WARNING, "Errore numeri: {0}", ex.getMessage());
            }
        }

        private MenuProduct buildProductFromForm() {
            String nome = txtName.getText();
            String tipo = cmbType.getValue();
            double prezzo = parseDoubleValue(txtPrice.getText());
            double costo = parseDoubleValue(txtCost.getText());
            String allergeni = txtAllergens.getText();

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
                    ? DatabaseService.updateProduct(product)
                    : DatabaseService.addProduct(product);
        }
    }
}