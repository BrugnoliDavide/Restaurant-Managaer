package com.example.rm.model;


import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderItem {

    private int id;
    private MenuProduct product;
    private int quantita;
    private BigDecimal  prezzoSnapshot;
    private BigDecimal costoSnapshot;
    private String nomeSnapshot;
    private String status = "active";

    public OrderItem() {
    }

    public OrderItem(MenuProduct product, int quantita, BigDecimal  prezzoSnapshot,
                     BigDecimal  costoSnapshot, String nomeSnapshot) {
        this.product = product;
        this.quantita = quantita;
        this.prezzoSnapshot = prezzoSnapshot;
        this.costoSnapshot = costoSnapshot;
        this.nomeSnapshot = nomeSnapshot;
    }

    public MenuProduct getProduct() {
        return product;
    }

    public void setProduct(MenuProduct product) {
        this.product = product;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }

    public BigDecimal getPrezzoSnapshot() {
        return prezzoSnapshot;
    }

    public void setPrezzoSnapshot(BigDecimal  prezzoSnapshot) {
        this.prezzoSnapshot = prezzoSnapshot;
    }

    public BigDecimal  getCostoSnapshot() {
        return costoSnapshot;
    }

    public void setCostoSnapshot(BigDecimal  costoSnapshot) {
        this.costoSnapshot = costoSnapshot;
    }

    public String getNomeSnapshot() {
        return nomeSnapshot;
    }

    public void setNomeSnapshot(String nomeSnapshot) {
        this.nomeSnapshot = nomeSnapshot;
    }

    public String getDisplayName() {
        if (nomeSnapshot != null && !nomeSnapshot.isEmpty()) {
            return nomeSnapshot;
        }
        return product != null ? product.getNome() : "Prodotto sconosciuto";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("%dx %s (€%s)", quantita, getDisplayName(),
                prezzoSnapshot.setScale(2, RoundingMode.HALF_UP));
    }

}