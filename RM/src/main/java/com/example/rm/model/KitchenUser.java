package com.example.rm.model;

public class KitchenUser extends User {
    public KitchenUser(String username) {
        super(username, "cucina");
    }

    @Override
    public String getWelcomeMessage() {
        return "Pronti a cucinare, Chef " + username;
    }
}