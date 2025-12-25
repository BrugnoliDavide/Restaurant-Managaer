package com.example.rm.view;

import com.example.rm.model.User;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.SecurityService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;


import java.util.logging.Level;

import static com.example.rm.view.LoginController.logger;

public class UsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TextField txtUser;
    @FXML private TextField txtPass; // Nel FXML usiamo TextField, ma idealmente sarebbe PasswordField
    @FXML private ComboBox<String> comboRole;
    @FXML private Parent rootPane;


    @FXML
    public void initialize() {
        // 1. Inizializza il menu a tendina dei ruoli
        comboRole.setItems(FXCollections.observableArrayList("manager", "cameriere", "cucina"));

        // 2. Carica i dati nella tabella
        loadData();
    }

    private void loadData() {
        // Recupera la lista utenti (che ora contiene solo username e ruolo)
        usersTable.setItems(FXCollections.observableArrayList(DatabaseService.getAllUsers()));
    }

    @FXML
    private void handleAdd() {
        // 1. Prendi i valori dagli input
        String u = txtUser.getText().trim();
        String p = txtPass.getText().trim();
        String r = comboRole.getValue();

        // 2. Validazione semplice
        if (u.isEmpty() || p.isEmpty() || r == null) {
            showAlert("Dati mancanti", "Per favore compila Username, Password e Ruolo.");
            return;
        }

        // 3. REGISTRAZIONE SICURA
        // Usiamo SecurityService per hashare la password prima di salvarla
        boolean ok = SecurityService.registerUser(u, p, r.toLowerCase());

        if (ok) {
            // Reset campi e ricarica tabella
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
            // Evitiamo di cancellare noi stessi o l'admin principale per sbaglio
            if (selected.getUsername().equals("admin")) {
                showAlert("Azione Negata", "Non puoi cancellare l'utente admin principale.");
                return;
            }

            // 2. Cancella dal DB


            boolean ok = DatabaseService.deleteUser(selected.getUsername());
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

        View managerView = ViewFactory.forRole("manager");

        rootPane
                .getScene()
                .setRoot(managerView.getRoot());
    }

    // Helper per mostrare messaggi
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}