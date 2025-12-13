package com.example.demo.app;

import com.example.demo.model.*;

public class UsersFactory {


    
    // Factory Method Pattern
    public static User createUser(String username, String role) {
        if (role == null) return null;

        switch (role.toLowerCase()) {
            case "manager":
                return new ManagerUser(username);
            case "cameriere":
                return new WaiterUser(username);
            case "cucina":
                return new KitchenUser(username);
            default:
                // Fallback: se il ruolo è sconosciuto, creiamo un utente generico anonimo
                // (Oppure lanciamo eccezione)
                return new User(username, role) {
                    @Override
                    public String getWelcomeMessage() {
                        return "Ciao " + username;
                    }
                };
        }
    }
}