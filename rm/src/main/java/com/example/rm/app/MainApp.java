package com.example.rm.app;

import com.example.rm.service.ConnectionManager;
import com.example.rm.view.LoginController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;


public class MainApp extends Application {


    private static final Logger logger = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) throws IOException {
        ConnectionManager.loadFromPreferences();
        boolean dbOk = ConnectionManager.testConnection();
        AppStatus.setDbConnectionOk(dbOk);

        Parent root = LoginController.getFXMLView();
        SceneManager.init(primaryStage);
        primaryStage.setScene(new Scene(root));

        primaryStage.setTitle("Restaurant Manager");
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.setResizable(true);
        setLogo(primaryStage);
        primaryStage.show();
    }


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