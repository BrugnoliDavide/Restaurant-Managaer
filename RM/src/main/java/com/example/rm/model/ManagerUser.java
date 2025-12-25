package com.example.rm.model;

public class ManagerUser extends User {
    public ManagerUser(String username) {
        super(username, "manager");
    }

    @Override
    public String getWelcomeMessage() {
        return "Bentornato, " + username + "!";
    }
}