package com.example.demo.model;

public class OrderItem {
    private int id;
    private int orderId;
    private MenuProduct product; // Contiene ID articolo, nome, ecc.
    private int quantita;
    private double prezzoSnapshot;
    private double costoSnapshot;

    // Costruttore
    public OrderItem(MenuProduct product, int quantita) {
        this.product = product;
        this.quantita = quantita;
        // Congeliamo i prezzi al momento della creazione
        this.prezzoSnapshot = product.getPrezzoVendita();
        this.costoSnapshot = product.getCostoRealizzazione();
    }

    // Getter
    public MenuProduct getProduct() { return product; }
    public int getQuantita() { return quantita; }
    public double getPrezzoSnapshot() { return prezzoSnapshot; }
    public double getCostoSnapshot() { return costoSnapshot; }
}