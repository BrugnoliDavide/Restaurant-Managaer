package com.example.rm.model;

import javafx.beans.property.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Order implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IntegerProperty id;
    private final ObjectProperty<LocalDateTime> dataOra;
    private final IntegerProperty tavolo;
    private final StringProperty username;
    private final StringProperty note;
    private final StringProperty status;
    private final DoubleProperty totale;

    // COSTRUTTORE VUOTO
    public Order() {
        this.id = new SimpleIntegerProperty(0);
        this.dataOra = new SimpleObjectProperty<>(LocalDateTime.now());
        this.tavolo = new SimpleIntegerProperty(0);
        this.username = new SimpleStringProperty("");
        this.note = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("to-do");
        this.totale = new SimpleDoubleProperty(0.0);
    }

    // COSTRUTTORE COMPLETO (MANTIENI FIRMA IDENTICA)
    public Order(int id, LocalDateTime dataOra, int tavolo,
                 String username, String note, String status, double totale) {
        this.id = new SimpleIntegerProperty(id);
        this.dataOra = new SimpleObjectProperty<>(dataOra);
        this.tavolo = new SimpleIntegerProperty(tavolo);
        this.username = new SimpleStringProperty(username);
        this.note = new SimpleStringProperty(note != null ? note : "");
        this.status = new SimpleStringProperty(status);
        this.totale = new SimpleDoubleProperty(totale);
    }

    // === GETTER/SETTER (MANTIENI FIRME IDENTICHE) ===

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public LocalDateTime getDataOra() { return dataOra.get(); }
    public void setDataOra(LocalDateTime dataOra) { this.dataOra.set(dataOra); }
    public ObjectProperty<LocalDateTime> dataOraProperty() { return dataOra; }

    public int getTavolo() { return tavolo.get(); }
    public void setTavolo(int tavolo) { this.tavolo.set(tavolo); }
    public IntegerProperty tavoloProperty() { return tavolo; }

    public String getUsername() { return username.get(); }
    public void setUsername(String username) { this.username.set(username); }
    public StringProperty usernameProperty() { return username; }

    public String getNote() { return note.get(); }
    public void setNote(String note) { this.note.set(note != null ? note : ""); }
    public StringProperty noteProperty() { return note; }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }

    public double getTotale() { return totale.get(); }
    public void setTotale(double totale) { this.totale.set(totale); }
    public DoubleProperty totaleProperty() { return totale; }

    // === METODI UTILITY (NUOVI) ===

    public String getDataOraFormatted() {
        return dataOra.get() != null ? dataOra.get().format(FORMATTER) : "";
    }

    public boolean hasNote() {
        String n = getNote();
        return n != null && !n.trim().isEmpty();
    }

    public boolean isPending() {
        return "to-do".equals(getStatus());
    }

    public boolean isReady() {
        return "ready".equals(getStatus());
    }

    public boolean isClosed() {
        return "closed".equals(getStatus());
    }

    @Override
    public String toString() {
        return String.format("Ordine #%d - Tavolo %d - €%.2f",
                getId(), getTavolo(), getTotale());
    }
}