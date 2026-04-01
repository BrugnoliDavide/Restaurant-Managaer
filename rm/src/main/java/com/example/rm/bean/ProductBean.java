package com.example.rm.bean;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Java Bean che rappresenta un prodotto del menù.
 * Corrisponde alla tabella {@code menu_items} del database.
 *
 * <p>Questa classe segue lo standard Java Bean:
 * costruttore no-arg, campi privati, getter e setter pubblici.</p>
 */
public class ProductBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int    id;
    private String nome;
    private String tipologia;
    private double prezzoVendita;
    private double costoRealizzazione;
    private String allergeni;

    // ------------------------------------------------------------------ //
    //  Costruttori                                                         //
    // ------------------------------------------------------------------ //

    /** Costruttore no-arg richiesto dallo standard Java Bean. */
    public ProductBean() {}

    /**
     * Costruttore di comodo per inizializzare tutti i campi in una sola istruzione.
     *
     * @param id                   identificatore univoco del prodotto
     * @param nome                 nome del prodotto
     * @param tipologia            categoria/tipologia (es. "primo", "bevanda")
     * @param prezzoVendita        prezzo di vendita in euro
     * @param costoRealizzazione   costo di produzione in euro
     * @param allergeni            elenco allergeni (può essere stringa vuota, mai {@code null})
     */
    public ProductBean(int id, String nome, String tipologia,
                       double prezzoVendita, double costoRealizzazione,
                       String allergeni) {
        this.id                 = id;
        this.nome               = nome;
        this.tipologia          = tipologia;
        this.prezzoVendita      = prezzoVendita;
        this.costoRealizzazione = costoRealizzazione;
        this.allergeni          = allergeni != null ? allergeni : "";
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipologia() {
        return tipologia;
    }

    public void setTipologia(String tipologia) {
        this.tipologia = tipologia;
    }

    public double getPrezzoVendita() {
        return prezzoVendita;
    }

    public void setPrezzoVendita(double prezzoVendita) {
        this.prezzoVendita = prezzoVendita;
    }

    public double getCostoRealizzazione() {
        return costoRealizzazione;
    }

    public void setCostoRealizzazione(double costoRealizzazione) {
        this.costoRealizzazione = costoRealizzazione;
    }

    public String getAllergeni() {
        return allergeni;
    }

    public void setAllergeni(String allergeni) {
        this.allergeni = allergeni != null ? allergeni : "";
    }

    // ------------------------------------------------------------------ //
    //  Metodi derivati (sola lettura, non esposti come proprietà Bean)     //
    // ------------------------------------------------------------------ //

    /**
     * Calcola il margine lordo del prodotto.
     *
     * @return differenza tra prezzo di vendita e costo di realizzazione
     */
    public double getMargine() {
        return prezzoVendita - costoRealizzazione;
    }

    /**
     * Calcola la percentuale di margine sul prezzo di vendita.
     *
     * @return percentuale di margine; {@code 0.0} se il prezzo è zero
     */
    public double getPercentualeMargine() {
        if (prezzoVendita == 0.0) return 0.0;
        return (getMargine() / prezzoVendita) * 100.0;
    }

    // ------------------------------------------------------------------ //
    //  equals, hashCode, toString                                          //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductBean other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("ProductBean{id=%d, nome='%s', tipologia='%s', prezzoVendita=%.2f}",
                id, nome, tipologia, prezzoVendita);
    }
}