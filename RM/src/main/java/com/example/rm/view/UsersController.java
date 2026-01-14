package com.example.rm.view;

import com.example.rm.app.SceneManager;
import com.example.rm.model.User;
import com.example.rm.service.SecurityService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;


import java.util.logging.Level;

import static com.example.rm.view.LoginController.logger;
import com.example.rm.controller.ManagerUseCase;
import com.example.rm.controller.ManagerService;

public class UsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TextField txtUser;
    @FXML private TextField txtPass;
    @FXML private ComboBox<String> comboRole;
    @FXML private Parent rootPane;

    private static final ManagerUseCase managerUseCase = new ManagerService();

    @FXML
    public void initialize() {
        comboRole.setItems(FXCollections.observableArrayList("manager", "cameriere", "cucina", "cassiere"));
        loadData();
    }

    private void loadData() {
        usersTable.setItems(FXCollections.observableArrayList(managerUseCase.loadAllUsers()));
    }

    @FXML
    private void handleAdd() {
        String u = txtUser.getText().trim();
        String p = txtPass.getText().trim();
        String r = comboRole.getValue();

        if (u.isEmpty() || p.isEmpty() || r == null) {
            showAlert("Dati mancanti", "Per favore compila Username, Password e Ruolo.");
            return;
        }


        // SecurityService trova l'hash della password prima di salvarla
        boolean ok = SecurityService.registerUser(u, p, r.toLowerCase());

        if (ok) {

            txtUser.clear();
            txtPass.clear();
            comboRole.getSelectionModel().clearSelection();
            loadData();
            logger.log(Level.INFO,"Utente {0} creato con successo!", txtUser);

        } else {
            showAlert("Errore", "Impossibile creare l'utente. Forse lo username esiste già?");
        }
    }

    @FXML
    private void handleDelete() {
        // 1. Prendi l'utente selezionato nella tabella
        User selected = usersTable.getSelectionModel().getSelectedItem();

        if (selected != null) {


            if (selected.getUsername().equals("admin")) {
                showAlert("Azione Negata", "Non puoi cancellare l'utente admin principale.");
                return;
            }




            boolean ok = managerUseCase.deleteUser(selected.getUsername());
            if (ok) {
                loadData();
            } else {
                showAlert("Errore", "Impossibile cancellare l'utente.");
            }
        } else {
            showAlert("Nessuna selezione", "Seleziona un utente dalla tabella per cancellarlo.");
        }
    }


    @FXML
    private void goBack() {
        SceneManager.showManager();
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}