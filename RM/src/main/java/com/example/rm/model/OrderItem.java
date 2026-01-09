package com.example.rm.model;

/**
 * Rappresenta un articolo all'interno di un ordine.
 * Mantiene uno snapshot dei dati del prodotto al momento dell'ordine.
 */
public class OrderItem {

    private MenuProduct product;
    private int quantita;
    private double prezzoSnapshot;
    private double costoSnapshot;
    private String nomeSnapshot;  // ✅ NUOVO: Nome del prodotto al momento dell'ordine

    /* =======================
       CONSTRUCTORS
       ======================= */

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

    /* =======================
       GETTERS & SETTERS
       ======================= */

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

    /* =======================
       UTILITY METHODS
       ======================= */

    /**
     * Ottiene il nome da visualizzare.
     * Usa lo snapshot se disponibile, altrimenti il nome del prodotto.
     * @return Nome del prodotto
     */
    public String getDisplayName() {
        if (nomeSnapshot != null && !nomeSnapshot.isEmpty()) {
            return nomeSnapshot;
        }
        return product != null ? product.getNome() : "Prodotto sconosciuto";
    }

    /**
     * Calcola il totale per questo articolo.
     * @return Prezzo snapshot * quantità
     */
    public double getTotale() {
        return prezzoSnapshot * quantita;
    }

    /**
     * Calcola il costo totale per questo articolo.
     * @return Costo snapshot * quantità
     */
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