package com.example.rm.model;

import javafx.beans.property.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    // MODIFICA 1: Aggiunto 'transient' e rimosso 'final'
    private transient IntegerProperty id;
    private transient IntegerProperty orderId;
    private transient ObjectProperty<MenuProduct> product;
    private transient IntegerProperty quantita;
    private transient DoubleProperty prezzoSnapshot;
    private transient DoubleProperty costoSnapshot;

    // COSTRUTTORE VUOTO
    public OrderItem() {
        this.id = new SimpleIntegerProperty(0);
        this.orderId = new SimpleIntegerProperty(0);
        this.product = new SimpleObjectProperty<>();
        this.quantita = new SimpleIntegerProperty(1);
        this.prezzoSnapshot = new SimpleDoubleProperty(0.0);
        this.costoSnapshot = new SimpleDoubleProperty(0.0);
    }

    // COSTRUTTORE SEMPLIFICATO
    public OrderItem(MenuProduct product, int quantita) {
        this();
        this.product.set(product);
        this.quantita.set(quantita);
        if (product != null) {
            this.prezzoSnapshot.set(product.getPrezzoVendita());
            this.costoSnapshot.set(product.getCostoRealizzazione());
        }
    }

    // COSTRUTTORE COMPLETO
    public OrderItem(int id, int orderId, MenuProduct product,
                     int quantita, double prezzoSnap, double costoSnap) {
        this.id = new SimpleIntegerProperty(id);
        this.orderId = new SimpleIntegerProperty(orderId);
        this.product = new SimpleObjectProperty<>(product);
        this.quantita = new SimpleIntegerProperty(quantita);
        this.prezzoSnapshot = new SimpleDoubleProperty(prezzoSnap);
        this.costoSnapshot = new SimpleDoubleProperty(costoSnap);
    }

    // === MODIFICA 2: Metodi per la Serializzazione Manuale ===

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        // Salviamo i valori
        s.writeInt(getId());
        s.writeInt(getOrderId());
        // MenuProduct è ora serializzabile (grazie alla modifica precedente), quindi possiamo salvarlo direttamente
        s.writeObject(getProduct());
        s.writeInt(getQuantita());
        s.writeDouble(getPrezzoSnapshot());
        s.writeDouble(getCostoSnapshot());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        // Ricostruiamo le Property
        this.id = new SimpleIntegerProperty(s.readInt());
        this.orderId = new SimpleIntegerProperty(s.readInt());
        this.product = new SimpleObjectProperty<>((MenuProduct) s.readObject());
        this.quantita = new SimpleIntegerProperty(s.readInt());
        this.prezzoSnapshot = new SimpleDoubleProperty(s.readDouble());
        this.costoSnapshot = new SimpleDoubleProperty(s.readDouble());
    }

    // === GETTER/SETTER (INVARIATI) ===

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public int getOrderId() { return orderId.get(); }
    public void setOrderId(int orderId) { this.orderId.set(orderId); }
    public IntegerProperty orderIdProperty() { return orderId; }

    public MenuProduct getProduct() { return product.get(); }
    public void setProduct(MenuProduct product) { this.product.set(product); }
    public ObjectProperty<MenuProduct> productProperty() { return product; }

    public int getQuantita() { return quantita.get(); }
    public void setQuantita(int quantita) { this.quantita.set(quantita); }
    public IntegerProperty quantitaProperty() { return quantita; }

    public double getPrezzoSnapshot() { return prezzoSnapshot.get(); }
    public void setPrezzoSnapshot(double prezzo) { this.prezzoSnapshot.set(prezzo); }
    public DoubleProperty prezzoSnapshotProperty() { return prezzoSnapshot; }

    public double getCostoSnapshot() { return costoSnapshot.get(); }
    public void setCostoSnapshot(double costo) { this.costoSnapshot.set(costo); }
    public DoubleProperty costoSnapshotProperty() { return costoSnapshot; }



    public double getTotaleRiga() {
        return getQuantita() * getPrezzoSnapshot();
    }

    public double getCostoTotaleRiga() {
        return getQuantita() * getCostoSnapshot();
    }

    public double getMargineRiga() {
        return getTotaleRiga() - getCostoTotaleRiga();
    }

    @Override
    public String toString() {
        return String.format("%dx %s (€%.2f)",
                getQuantita(),
                product.get() != null ? product.get().getNome() : "N/A",
                getTotaleRiga());
    }
}