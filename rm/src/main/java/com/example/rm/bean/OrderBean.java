package com.example.rm.bean;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Java Bean che rappresenta un ordine.
 * Corrisponde alla tabella {@code orders} del database.
 *
 * <p>La lista degli articoli ({@link OrderItemBean}) è opzionale:
 * può rimanere vuota se il Bean è usato solo per rappresentare
 * l'intestazione dell'ordine senza il dettaglio delle righe.</p>
 */
public class OrderBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Valori ammessi per il campo {@code status}. */
    public static final String STATUS_TODO      = "to-do";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_CANCELED  = "canceled";

    private int             id;
    private LocalDateTime   dataOra;
    private int             tavolo;
    private String          username;
    private String          note;
    private String          status;
    private double          totale;
    private List<OrderItemBean> items;

    // ------------------------------------------------------------------ //
    //  Costruttori                                                         //
    // ------------------------------------------------------------------ //

    /** Costruttore no-arg richiesto dallo standard Java Bean. */
    public OrderBean() {
        this.dataOra = LocalDateTime.now();
        this.status  = STATUS_TODO;
        this.note    = "";
        this.items   = new ArrayList<>();
    }

    /**
     * Costruttore di comodo senza lista articoli.
     *
     * @param id        identificatore univoco dell'ordine
     * @param dataOra   data e ora di creazione
     * @param tavolo    numero del tavolo
     * @param username  username del cameriere che ha preso l'ordine
     * @param note      eventuali note (può essere stringa vuota)
     * @param status    stato corrente dell'ordine
     * @param totale    totale calcolato in euro
     */
    public OrderBean(int id, LocalDateTime dataOra, int tavolo,
                     String username, String note,
                     String status, double totale) {
        this.id       = id;
        this.dataOra  = dataOra;
        this.tavolo   = tavolo;
        this.username = username;
        this.note     = note != null ? note : "";
        this.status   = status;
        this.totale   = totale;
        this.items    = new ArrayList<>();
    }

    // ------------------------------------------------------------------ //
    //  Getter e Setter                                                     //
    // ------------------------------------------------------------------ //

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public void setDataOra(LocalDateTime dataOra) {
        this.dataOra = dataOra;
    }

    public int getTavolo() {
        return tavolo;
    }

    public void setTavolo(int tavolo) {
        this.tavolo = tavolo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note != null ? note : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotale() {
        return totale;
    }

    public void setTotale(double totale) {
        this.totale = totale;
    }

    /**
     * Restituisce una vista non modificabile della lista degli articoli.
     * Per aggiungere o rimuovere articoli usare {@link #addItem} e {@link #removeItem}.
     *
     * @return lista immutabile degli articoli dell'ordine
     */
    public List<OrderItemBean> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<OrderItemBean> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    // ------------------------------------------------------------------ //
    //  Metodi di utilità                                                   //
    // ------------------------------------------------------------------ //

    /** Aggiunge un articolo all'ordine. */
    public void addItem(OrderItemBean item) {
        if (item != null) this.items.add(item);
    }

    /** Rimuove un articolo dall'ordine. */
    public void removeItem(OrderItemBean item) {
        this.items.remove(item);
    }

    /** Indica se l'ordine contiene note non vuote. */
    public boolean hasNote() {
        return note != null && !note.trim().isEmpty();
    }

    // ------------------------------------------------------------------ //
    //  equals, hashCode, toString                                          //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderBean other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("OrderBean{id=%d, tavolo=%d, status='%s', totale=%.2f}",
                id, tavolo, status, totale);
    }
}