package com.example.rm.app;

import com.example.rm.exception.InvalidUserSessionException;
import com.example.rm.model.User;
import javafx.scene.Scene;

public class UserSession {

    private static UserSession instance;
    private final User user;

    private UserSession(User user) {
        this.user = user;
    }

    // Singleton
    public static UserSession getInstance(User user) {
        if (instance == null) {
            instance = new UserSession(user);
        }
        return instance;
    }

    // Singleton già esistente
    public static UserSession getInstance() {
        if (instance == null) {
            throw new InvalidUserSessionException();
        }
        return instance;
    }

    public static void cleanUserSession() {
        instance = null;
        SceneManager.clearViewCache();
    }

    public User getUser() {
        return user;
    }

    private java.util.Set<Integer> managedTables = null;

    public void setManagedTables(java.util.Set<Integer> tables) {
        this.managedTables = tables;
    }

    public boolean isTableManaged(int tableNumber) {
        if (managedTables == null || managedTables.isEmpty()) {
            return true;
        }
        return managedTables.contains(tableNumber);
    }
}
