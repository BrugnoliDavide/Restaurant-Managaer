package com.example.rm.app;

import com.example.rm.model.*;

public class UsersFactory {

    private UsersFactory() {
        throw new IllegalStateException("Utility class");
   }
    
    // Factory Method
    public static User createUser(String username, String role) {

        switch (role.toLowerCase()) {
            case "manager":
                return new ManagerUser(username);
            case "cameriere":
                return new WaiterUser(username);
            case "cucina":
                return new KitchenUser(username);
            case "":
                throw new IllegalArgumentException("Il ruolo non può essere vuoto");

            default:
                return new User(username, role) {
                    @Override
                    public String getWelcomeMessage() {
                        return "Ciao " + username;
                    }
                };
        }
    }
}