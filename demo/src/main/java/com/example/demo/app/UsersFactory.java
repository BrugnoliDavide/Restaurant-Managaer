package com.example.demo.app;

import com.example.demo.model.*;

public class UsersFactory {

    private UsersFactory() {
        throw new IllegalStateException("Utility class");
        //trattandosi di un eccezione standard non è necessario "scriverla"
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
                return null;

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