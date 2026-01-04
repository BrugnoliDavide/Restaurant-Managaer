package com.example.rm.model;

import javafx.beans.property.*;
import java.io.Serializable;

public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final IntegerProperty id;
    private final IntegerProperty orderId;
    private final ObjectProperty<MenuProduct> product;
    private final IntegerProperty quantita;
    private final DoubleProperty prezzoSnapshot;
    private final DoubleProperty costoSnapshot;

    // COSTRUTTORE VUOTO
    public OrderItem() {
        this.id = new SimpleIntegerProperty(0);
        this.orderId = new SimpleIntegerProperty(0);
        this.product = new SimpleObjectProperty<>();
        this.quantita = new SimpleIntegerProperty(1);
        this.prezzoSnapshot = new SimpleDoubleProperty(0.0);
        this.costoSnapshot = new SimpleDoubleProperty(0.0);
    }

    // COSTRUTTORE SEMPLIFICATO (MANTIENI FIRMA IDENTICA)
    public OrderItem(MenuProduct product, int quantita) {
        this();
        this.product.set(product);
        this.quantita.set(quantita);
        if (product != null) {
            this.prezzoSnapshot.set(product.getPrezzoVendita());
            this.costoSnapshot.set(product.getCostoRealizzazione());
        }
    }

    // COSTRUTTORE COMPLETO (per future query dal DB)
    public OrderItem(int id, int orderId, MenuProduct product,
                     int quantita, double prezzoSnap, double costoSnap) {
        this.id = new SimpleIntegerProperty(id);
        this.orderId = new SimpleIntegerProperty(orderId);
        this.product = new SimpleObjectProperty<>(product);
        this.quantita = new SimpleIntegerProperty(quantita);
        this.prezzoSnapshot = new SimpleDoubleProperty(prezzoSnap);
        this.costoSnapshot = new SimpleDoubleProperty(costoSnap);
    }

    // === GETTER/SETTER (MANTIENI FIRME IDENTICHE) ===

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

    // === METODI CALCOLATI (NUOVI) ===

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