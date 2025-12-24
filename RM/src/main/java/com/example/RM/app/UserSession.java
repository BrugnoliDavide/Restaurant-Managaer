package com.example.RM.app;

import com.example.RM.model.User;

public class UserSession {

    private static UserSession instance;
    private User user; // Qui salviamo l'oggetto "astratto" (che può essere Manager, Waiter, ecc.)

    // Costruttore privato
    private UserSession(User user) {
        this.user = user;
    }

    // Singleton: Crea o recupera l'istanza
    public static UserSession getInstance(User user) {
        if (instance == null) {
            instance = new UserSession(user);
        }
        return instance;
    }

    // Singleton: Recupera l'istanza esistente
    public static UserSession getInstance() {
        return instance;
    }

    // Logout: Pulisce tutto
    public static void cleanUserSession() {
        instance = null;
    }

    // Getter dell'utente corrente
    public User getUser() {
        return user;
    }


}