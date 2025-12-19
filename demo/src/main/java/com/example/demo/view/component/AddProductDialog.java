package com.example.demo.view.component;

import com.example.demo.app.MainApp;
import com.example.demo.model.MenuProduct;
import com.example.demo.service.DatabaseService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddProductDialog {


    public static void display() {
        showDialog(null);
    }


    public static void displayEdit(MenuProduct productToEdit) {
        showDialog(productToEdit);
    }


    private static void showDialog(MenuProduct productToEdit) {
        boolean isEditMode = (productToEdit != null);

        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(isEditMode ? "Modifica Prodotto" : "Aggiungi Prodotto");
        window.setMinWidth(300);

        MainApp.setLogo(window);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");


        TextField txtName = new TextField(isEditMode ? productToEdit.getNome() : "");
        txtName.setPromptText("Nome");

        ComboBox<String> cmbType = new ComboBox<>();
        cmbType.getItems().addAll(DatabaseService.getAllCategories());
        cmbType.setEditable(true);
        if (isEditMode) cmbType.setValue(productToEdit.getTipologia());
        else if (!cmbType.getItems().isEmpty()) cmbType.getSelectionModel().selectFirst();

        TextField txtPrice = new TextField(isEditMode ? String.valueOf(productToEdit.getPrezzoVendita()) : "");
        txtPrice.setPromptText("Prezzo Vendita");

        TextField txtCost = new TextField(isEditMode ? String.valueOf(productToEdit.getCostoRealizzazione()) : "");
        txtCost.setPromptText("Costo Realizzazione");

        TextField txtAllergens = new TextField(isEditMode ? productToEdit.getAllergeni() : "");
        txtAllergens.setPromptText("Allergeni");

        Button btnSave = new Button(isEditMode ? "Salva Modifiche" : "Aggiungi al Menu");
        btnSave.setStyle("-fx-background-color: #2B2B2B; -fx-text-fill: white; -fx-cursor: hand;");
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            try {
                String nome = txtName.getText();
                String tipo = cmbType.getValue();
                double prezzo = Double.parseDouble(txtPrice.getText().replace(",", "."));
                double costo = Double.parseDouble(txtCost.getText().replace(",", "."));
                String all = txtAllergens.getText();

                boolean success;
                if (isEditMode) {
                    // MODIFICA: Mantieni l'ID originale!
                    MenuProduct updated = new MenuProduct(productToEdit.getId(), nome, tipo, prezzo, costo, all);
                    success = DatabaseService.updateProduct(updated);
                } else {
                    // AGGIUNTA: Nuovo prodotto
                    MenuProduct nuovo = new MenuProduct(nome, tipo, prezzo, costo, all);
                    success = DatabaseService.addProduct(nuovo);
                }

                if (success) window.close();
                else System.out.println("Errore salvataggio DB");

            } catch (NumberFormatException ex) {
                System.out.println("Errore numeri: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(
                new Label("Nome:"), txtName,
                new Label("Tipologia:"), cmbType,
                new Label("Prezzo (€):"), txtPrice,
                new Label("Costo (€):"), txtCost,
                new Label("Allergeni:"), txtAllergens,
                new Label(""), btnSave
        );

        window.setScene(new Scene(layout));
        window.showAndWait();
    }
}