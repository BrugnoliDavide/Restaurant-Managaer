package com.example.rm.app;

import com.example.rm.exception.InvalidUserSessionException;
import com.example.rm.model.User;

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


    /**
     * metodi esclusivo per i test
            * @param testUser Utente da utilizzare nei test
     */
    public static void setInstanceForTesting(User testUser) {
        instance = new UserSession(testUser);
    }

    /**
     *
     * ATTENZIONE: Questo metodo NON deve essere utilizzato nel codice di produzione.
     */
    public static void clearInstanceForTesting() {
        instance = null;
    }
}

