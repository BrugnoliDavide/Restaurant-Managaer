package com.example.demo.model;

public abstract class User {
    protected String username;
    protected String role;

    protected User(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }

    public abstract String getWelcomeMessage();
}