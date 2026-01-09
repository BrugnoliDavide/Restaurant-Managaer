package com.example.rm.view.component;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangePasswordDialog {

    private static final Logger logger = Logger.getLogger(ChangePasswordDialog.class.getName());

    private ChangePasswordDialog() {}

    public static void show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChangePasswordDialog.class.getResource("ChangePasswordDialog.fxml"));
            VBox root = loader.load();
            ChangePasswordDialogController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(owner);
            dialog.setTitle("Cambia Password");
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            controller.setStage(dialog);

            dialog.showAndWait();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento ChangePasswordDialog.fxml", e);
        }
    }
}
