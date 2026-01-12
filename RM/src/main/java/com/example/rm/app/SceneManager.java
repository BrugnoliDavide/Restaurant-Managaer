package com.example.rm.app;

import com.example.rm.view.View;
import com.example.rm.view.ViewFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void showLogin() {
        primaryStage.setScene(new Scene(ViewFactory.getLoginView().getRoot()));
    }

    public static void showMenu() {
        primaryStage.setScene(new Scene(ViewFactory.forRole("manager").getRoot()));
    }

    public static void showOrders() {
        primaryStage.setScene(new Scene(ViewFactory.forRole("waiter").getRoot()));
    }

    // metodo generico per qualsiasi ruolo
    public static void showView(String role) {
        primaryStage.setScene(new Scene(ViewFactory.forRole(role).getRoot()));
    }
}
