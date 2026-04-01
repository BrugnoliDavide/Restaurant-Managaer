package com.example.rm.bean;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Java Bean che rappresenta una singola riga (articolo) di un ordine.
 * Corrisponde alla tabella {@code order_items} del database.
 *
 * <p>I campi {@code *Snapshot} conservano i valori di prezzo e nome
 * al momento dell'ordine, in modo che eventuali modifiche successive
 * al menù non alterino lo storico degli ordini già registrati.</p>
 */
public class OrderItemBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int    orderId;
    private int    menuItemId;
    private int    quantita;
    private double prezzoVenditaSnapshot;
    private double costoRealizzazioneSnapshot;
    private String nomeProdottoSnapshot;

    // ------------------------------------------------------------------ //
    //  Costruttori                                                         //
    // ------------------------------------------------------------------ //

    /** Costruttore no-arg richiesto dallo standard Java Bean. */
    public OrderItemBean() {}

    /**
     * Costruttore di comodo per inizializzare tutti i campi.
     *
     * @param orderId                      ID dell'ordine padre
     * @param menuItemId                   ID del prodotto nel menù
     * @param quantita                     quantità ordinata
     * @param prezzoVenditaSnapshot        prezzo unitario al momento dell'ordine
     * @param costoRealizzazioneSnapshot   costo unitario al momento dell'ordine
     * @param nomeProdottoSnapshot         nome del prodotto al momento dell'ordine
     */
    public OrderItemBean(int orderId, int menuItemId, int quantita,
                         double prezzoVenditaSnapshot,
                         double costoRealizzazioneSnapshot,
                         String nomeProdottoSnapshot) {
        this.orderId                     = orderId;
        this.menuItemId                  = menuItemId;
        this.quantita                    = quantita;
        this.prezzoVenditaSnapshot       = prezzoVenditaSnapshot;
        this.costoRealizzazioneSnapshot  = costoRealizzazioneSnapshot;
        this.nomeProdottoSnapshot        = nomeProdottoSnapshot != null ? nomeProdottoSnapshot : "";
    }

    // ------------------------------------------------------------------ //
    //  Getter e Setter                                                     //
    // ------------------------------------------------------------------ //

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }

    public double getPrezzoVenditaSnapshot() {
        return prezzoVenditaSnapshot;
    }

    public void setPrezzoVenditaSnapshot(double prezzoVenditaSnapshot) {
        this.prezzoVenditaSnapshot = prezzoVenditaSnapshot;
    }

    public double getCostoRealizzazioneSnapshot() {
        return costoRealizzazioneSnapshot;
    }

    public void setCostoRealizzazioneSnapshot(double costoRealizzazioneSnapshot) {
        this.costoRealizzazioneSnapshot = costoRealizzazioneSnapshot;
    }

    public String getNomeProdottoSnapshot() {
        return nomeProdottoSnapshot;
    }

    public void setNomeProdottoSnapshot(String nomeProdottoSnapshot) {
        this.nomeProdottoSnapshot = nomeProdottoSnapshot != null ? nomeProdottoSnapshot : "";
    }

    // ------------------------------------------------------------------ //
    //  Metodi di utilità                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Calcola il subtotale della riga (quantità × prezzo unitario snapshot).
     *
     * @return subtotale in euro
     */
    public double getSubtotale() {
        return quantita * prezzoVenditaSnapshot;
    }

    // ------------------------------------------------------------------ //
    //  equals, hashCode, toString                                          //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemBean other)) return false;
        return orderId == other.orderId && menuItemId == other.menuItemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, menuItemId);
    }

    @Override
    public String toString() {
        return String.format("OrderItemBean{orderId=%d, menuItemId=%d, nome='%s', quantita=%d, subtotale=%.2f}",
                orderId, menuItemId, nomeProdottoSnapshot, quantita, getSubtotale());
    }
}