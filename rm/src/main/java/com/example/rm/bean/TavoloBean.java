package com.example.rm.bean;

import java.io.Serial;
import java.io.Serializable;

/**
 * Java Bean che rappresenta un tavolo del ristorante.
 *
 * <p>Nel database attuale il tavolo è modellato come semplice intero
 * nella colonna {@code orders.tavolo}. Questa classe introduce il
 * concetto di {@code Tavolo} come entità di dominio autonoma,
 * predisponendo la struttura per una futura tabella {@code tavoli}.</p>
 *
 * <p>Lo stato ({@code status}) riflette la condizione operativa del tavolo:</p>
 * <ul>
 *   <li>{@link #STATUS_FREE}    – nessun ordine attivo</li>
 *   <li>{@link #STATUS_OCCUPIED} – almeno un ordine non consegnato</li>
 *   <li>{@link #STATUS_WAITING}  – ordine consegnato, conto non ancora emesso</li>
 * </ul>
 */
public class TavoloBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Valori ammessi per il campo {@code status}. */
    public static final String STATUS_FREE     = "libero";
    public static final String STATUS_OCCUPIED = "occupato";
    public static final String STATUS_WAITING  = "in attesa di conto";

    private int    numero;
    private int    capienza;
    private String status;
    private String note;

    // ------------------------------------------------------------------ //
    //  Costruttori                                                         //
    // ------------------------------------------------------------------ //

    /** Costruttore no-arg richiesto dallo standard Java Bean. */
    public TavoloBean() {
        this.status = STATUS_FREE;
        this.note   = "";
    }

    /**
     * Costruttore di comodo con i campi principali.
     *
     * @param numero    numero identificativo del tavolo
     * @param capienza  numero massimo di coperti
     * @param status    stato corrente del tavolo
     */
    public TavoloBean(int numero, int capienza, String status) {
        this.numero   = numero;
        this.capienza = capienza;
        this.status   = status;
        this.note     = "";
    }

    /**
     * Costruttore completo.
     *
     * @param numero    numero identificativo del tavolo
     * @param capienza  numero massimo di coperti
     * @param status    stato corrente del tavolo
     * @param note      eventuali annotazioni operative (es. "riservato")
     */
    public TavoloBean(int numero, int capienza, String status, String note) {
        this.numero   = numero;
        this.capienza = capienza;
        this.status   = status;
        this.note     = note != null ? note : "";
    }

    // ------------------------------------------------------------------ //
    //  Getter e Setter                                                     //
    // ------------------------------------------------------------------ //

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getCapienza() {
        return capienza;
    }

    public void setCapienza(int capienza) {
        this.capienza = capienza;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note != null ? note : "";
    }

    // ------------------------------------------------------------------ //
    //  Metodi di utilità                                                   //
    // ------------------------------------------------------------------ //

    /** @return {@code true} se il tavolo è nello stato {@value #STATUS_FREE} */
    public boolean isFree() {
        return STATUS_FREE.equals(status);
    }

    /** @return {@code true} se il tavolo è nello stato {@value #STATUS_OCCUPIED} */
    public boolean isOccupied() {
        return STATUS_OCCUPIED.equals(status);
    }

    // ------------------------------------------------------------------ //
    //  equals, hashCode, toString                                          //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TavoloBean other)) return false;
        return numero == other.numero;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(numero);
    }

    @Override
    public String toString() {
        return String.format("TavoloBean{numero=%d, capienza=%d, status='%s'}",
                numero, capienza, status);
    }
}