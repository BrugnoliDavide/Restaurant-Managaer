package com.example.rm.app;

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


    public static UserSession getInstance() {
        return instance;
    }


    public static void cleanUserSession() {
        instance = null;
    }


    public User getUser() {
        return user;
    }

    private java.util.Set<Integer> managedTables = null;


    public void setManagedTables(java.util.Set<Integer> tables) {
        this.managedTables = tables;
    }

    public boolean isTableManaged(int tableNumber) {
        // Se è null, significa che non c'è filtro: gestisce TUTTI i tavoli
        if (managedTables == null || managedTables.isEmpty()) {
            return true;
        }
        return managedTables.contains(tableNumber);
    }

/*
    public String getManagedTablesString() {
        // Utile per ripopolare la casella di testo quando la riapri
        if (managedTables == null || managedTables.isEmpty()) return "";
        return managedTables.toString();
    }*/




}