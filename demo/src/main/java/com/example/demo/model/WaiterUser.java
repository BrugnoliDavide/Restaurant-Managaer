package com.example.demo.model;

public class WaiterUser extends User {
    public WaiterUser(String username) {
        super(username, "cameriere");
    }

    @Override
    public String getWelcomeMessage() {
        return "Buon lavoro ai tavoli, " + username;
    }
}