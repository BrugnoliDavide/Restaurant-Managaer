package com.example.rm.model;

import com.example.rm.service.OrderService;
import javafx.beans.property.*;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient IntegerProperty id;
    private transient ObjectProperty<LocalDateTime> dataOra;
    private transient IntegerProperty tavolo;
    private transient StringProperty username;
    private transient StringProperty note;
    private transient StringProperty status;
    private transient ObjectProperty<BigDecimal> totale;

    public Order() {
        this.id = new SimpleIntegerProperty(0);
        this.dataOra = new SimpleObjectProperty<>(LocalDateTime.now());
        this.tavolo = new SimpleIntegerProperty(0);
        this.username = new SimpleStringProperty("");
        this.note = new SimpleStringProperty("");
        this.status = new SimpleStringProperty("to-do");
        this.totale = new SimpleObjectProperty<>(BigDecimal.ZERO);
    }

    public Order(int id, LocalDateTime dataOra, int tavolo,
                 String username, String note, String status, BigDecimal totale) {
        this.id = new SimpleIntegerProperty(id);
        this.dataOra = new SimpleObjectProperty<>(dataOra);
        this.tavolo = new SimpleIntegerProperty(tavolo);
        this.username = new SimpleStringProperty(username);
        this.note = new SimpleStringProperty(note != null ? note : "");
        this.status = new SimpleStringProperty(status);
        this.totale = new SimpleObjectProperty<>(totale);
    }

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(getId());
        s.writeObject(getDataOra());
        s.writeInt(getTavolo());
        s.writeUTF(getUsername() != null ? getUsername() : "");
        s.writeUTF(getNote() != null ? getNote() : "");
        s.writeUTF(getStatus() != null ? getStatus() : "");
        s.writeUTF(getTotale().toPlainString());
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.id = new SimpleIntegerProperty(s.readInt());
        this.dataOra = new SimpleObjectProperty<>((LocalDateTime) s.readObject());
        this.tavolo = new SimpleIntegerProperty(s.readInt());
        this.username = new SimpleStringProperty(s.readUTF());
        this.note = new SimpleStringProperty(s.readUTF());
        this.status = new SimpleStringProperty(s.readUTF());
        this.totale = new SimpleObjectProperty<>(new BigDecimal(s.readUTF()));
    }

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

    public LocalDateTime getDataOra() { return dataOra.get(); }

    public int getTavolo() { return tavolo.get(); }
    public void setTavolo(int tavolo) { this.tavolo.set(tavolo); }

    public String getUsername() { return username.get(); }
    public void setUsername(String username) { this.username.set(username); }

    public String getNote() { return note.get(); }
    public void setNote(String note) { this.note.set(note != null ? note : ""); }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }

    public BigDecimal getTotale() { return totale.get(); }

    public boolean hasNote() {
        String n = getNote();
        return n != null && !n.trim().isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Ordine #%d - Tavolo %d - €%.2f",
                getId(), getTavolo(), getTotale());
    }

    private transient List<String> displayItems;  // Lazy
    public List<String> getDisplayItems() {
        if (displayItems == null) {
            displayItems = OrderService.getItemsForDisplay(getId());
        }
        return displayItems;
    }

    public void setDisplayItems(List<String> displayItems) {
        this.displayItems = displayItems;
    }
}