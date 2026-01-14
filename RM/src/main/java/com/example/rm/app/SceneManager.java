package com.example.rm.app;

import com.example.rm.view.View;
import com.example.rm.view.ViewFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneManager {

    private static Stage primaryStage;
    private static final Map<String, View> viewCache = new HashMap<>();
    public static final Logger logger = Logger.getLogger(SceneManager.class.getName());

    private static Scene currentScene;

    private SceneManager() {}

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
        if (primaryStage == null) {
            logger.log(Level.WARNING, "SceneManager is null");
            return;
        }
        primaryStage.setScene(new Scene(ViewFactory.forRole("menu").getRoot()));
        currentScene = primaryStage.getScene();
    }

    public static void showFinancial() {
        logger.log(Level.INFO,"avvio tentativo recupero storico ordini");
        if (currentScene == null) {
            logger.log(Level.WARNING, "currentScene null in showFinancial");
            return;
        }
        View financialView = getOrCreateView("financial");
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

    public static void showWaiter() {
        View usersView = getOrCreateView("cameriere");
        currentScene.setRoot(usersView.getRoot());
    }


    // metodo generico per qualsiasi ruolo
    public static void showView(String role) {
        primaryStage.setScene(new Scene(ViewFactory.forRole(role).getRoot()));
        currentScene = primaryStage.getScene();
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

    public static void showTakeOrder(int tavolo) {
        View takeOrderView = new com.example.rm.view.screens.TakeOrderView(tavolo);
        currentScene.setRoot(takeOrderView.getRoot());
    }
}
