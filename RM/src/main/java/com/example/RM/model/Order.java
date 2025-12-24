package com.example.RM.model;

import java.time.LocalDateTime;

public class Order {
    private int id;
    private LocalDateTime dataOra;
    private int tavolo;
    private String username;
    private String note;
    private String status;
    private double totale;

    // Costruttore completo (per la lettura dal DB)
    public Order(int id, LocalDateTime dataOra, int tavolo, String username, String note, String status, double totale) {
        this.id = id;
        this.dataOra = dataOra;
        this.tavolo = tavolo;
        this.username = username;
        this.note = note;
        this.status = status;
        this.totale = totale;
    }

    // Getter
    public int getId() { return id; }
    public LocalDateTime getDataOra() { return dataOra; }
    public int getTavolo() { return tavolo; }
    public String getUsername() { return username; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public double getTotale() { return totale; }

    // Setter per lo stato (utile per cambiarlo al volo)
    public void setStatus(String status) { this.status = status; }
}