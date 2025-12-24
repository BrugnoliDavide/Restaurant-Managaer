package com.example.RM.app;

import com.example.RM.service.DatabaseService;
import com.example.RM.view.LoginController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

import static com.example.RM.view.LoginController.logger;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 1. Carica il file FXML
        // Nota: Assicurati che LoginView.fxml sia dentro src/main/resources
        //FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginView.fxml"));
        //Parent root = loader.load();

        DatabaseService.loadFromPreferences();


        boolean dbOk = DatabaseService.testConnection();


        AppStatus.setDbConnectionOk(dbOk);

        Parent root = LoginController.getFXMLView();
        primaryStage.setScene(new Scene(root));


        primaryStage.setTitle("Restaurant Manager");

        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.setResizable(true);

        setLogo(primaryStage);

        primaryStage.show();




        primaryStage.show();


        /*
        // 2. Crea la scena
        Scene scene = new Scene(root, 450, 550);

        // 3. Carica il CSS (Fondamentale per lo stile)
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // 4. Configurazione Finestra
        primaryStage.setTitle("Restaurant Manager");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.setResizable(true);

        setLogo(primaryStage);

        primaryStage.show();
    }*/}

    public static void setLogo(Stage stage) {
        try {
            Image icon = new Image(MainApp.class.getResourceAsStream("/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            logger.warning("Logo non trovato.");
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        launch();
    }
}