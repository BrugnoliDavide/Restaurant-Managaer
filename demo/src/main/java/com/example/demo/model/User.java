package com.example.demo.model;

public class User {
    private String username;
    // Password rimossa: non deve mai uscire dal livello di servizio di autenticazione!
    private String role;

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}