package com.example.rm.app;

import com.example.rm.view.View;
import com.example.rm.view.ViewFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class SceneManager {

    private static Stage primaryStage;
    private static final Map<String, View> viewCache = new HashMap<>();

    private static Scene currentScene;

    public static void setCurrentScene(Scene scene) {
        currentScene = scene;
    }

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


    public static void showFinancial() {
        View financialView = getOrCreateView("financial");  // ← usa cache
        currentScene.setRoot(financialView.getRoot());
    }


    public static void showUsers() {
        View usersView = getOrCreateView("users");
        currentScene.setRoot(usersView.getRoot());
    }

    public static void showManager() {
        View usersView = getOrCreateView("manager");
        currentScene.setRoot(usersView.getRoot());
    }


    // metodo generico per qualsiasi ruolo
    public static void showView(String role) {
        primaryStage.setScene(new Scene(ViewFactory.forRole(role).getRoot()));
    }

    private static View getOrCreateView(String roleKey) {
        if (viewCache.containsKey(roleKey)) {
            return viewCache.get(roleKey);
        }

        View newView = ViewFactory.forRole(roleKey);
        viewCache.put(roleKey, newView);
        return newView;
    }

    public static void clearViewCache() {
        viewCache.clear();
    }


}
