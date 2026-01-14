package com.example.rm.model;


public class OrderItem {

    private MenuProduct product;
    private int quantita;
    private double prezzoSnapshot;
    private double costoSnapshot;
    private String nomeSnapshot;  // ✅ NUOVO: Nome del prodotto al momento dell'ordine



    public OrderItem() {
    }

    public OrderItem(MenuProduct product, int quantita, double prezzoSnapshot,
                     double costoSnapshot, String nomeSnapshot) {
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

    public double getPrezzoSnapshot() {
        return prezzoSnapshot;
    }

    public void setPrezzoSnapshot(double prezzoSnapshot) {
        this.prezzoSnapshot = prezzoSnapshot;
    }

    public double getCostoSnapshot() {
        return costoSnapshot;
    }

    public void setCostoSnapshot(double costoSnapshot) {
        this.costoSnapshot = costoSnapshot;
    }

    public String getNomeSnapshot() {
        return nomeSnapshot;
    }

    public void setNomeSnapshot(String nomeSnapshot) {
        this.nomeSnapshot = nomeSnapshot;
    }



    /**
     * @return Nome del prodotto
     */
    public String getDisplayName() {
        if (nomeSnapshot != null && !nomeSnapshot.isEmpty()) {
            return nomeSnapshot;
        }
        return product != null ? product.getNome() : "Prodotto sconosciuto";
    }

    /**
 .
     * @return Prezzo snapshot * quantità
     */
    public double getTotale() {
        return prezzoSnapshot * quantita;
    }


    public double getCostoTotale() {
        return costoSnapshot * quantita;
    }


    public double getMargine() {
        return (prezzoSnapshot - costoSnapshot) * quantita;
    }

    @Override
    public String toString() {
        return String.format("%dx %s (€%.2f)",
                quantita, getDisplayName(), prezzoSnapshot);
    }
}