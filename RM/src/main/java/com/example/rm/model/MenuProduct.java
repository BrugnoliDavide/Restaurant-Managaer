package com.example.rm.model;

import javafx.beans.property.*;
import java.io.Serializable;

public class MenuProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    // Properties JavaFX
    private final IntegerProperty id;
    private final StringProperty nome;
    private final StringProperty tipologia;
    private final DoubleProperty prezzoVendita;
    private final DoubleProperty costoRealizzazione;
    private final StringProperty allergeni;

    // COSTRUTTORE 1: Vuoto (JavaBean requirement)
    public MenuProduct() {
        this.id = new SimpleIntegerProperty(0);
        this.nome = new SimpleStringProperty("");
        this.tipologia = new SimpleStringProperty("");
        this.prezzoVendita = new SimpleDoubleProperty(0.0);
        this.costoRealizzazione = new SimpleDoubleProperty(0.0);
        this.allergeni = new SimpleStringProperty("");
    }

    // COSTRUTTORE 2: Completo (da DB - MANTIENI FIRMA IDENTICA)
    public MenuProduct(int id, String nome, String tipologia,
                       double prezzo, double costo, String allergeni) {
        this.id = new SimpleIntegerProperty(id);
        this.nome = new SimpleStringProperty(nome);
        this.tipologia = new SimpleStringProperty(tipologia);
        this.prezzoVendita = new SimpleDoubleProperty(prezzo);
        this.costoRealizzazione = new SimpleDoubleProperty(costo);
        this.allergeni = new SimpleStringProperty(allergeni != null ? allergeni : "");
    }

    // COSTRUTTORE 3: Senza ID (per nuovi prodotti)
    public MenuProduct(String nome, String tipologia,
                       double prezzo, double costo, String allergeni) {
        this(0, nome, tipologia, prezzo, costo, allergeni);
    }

    // COSTRUTTORE 4: Semplificato legacy
    public MenuProduct(String nome, String tipologia, double prezzo, double costo) {
        this(0, nome, tipologia, prezzo, costo, "");
    }

    // === GETTER E SETTER (MANTIENI FIRME IDENTICHE) ===

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getNome() { return nome.get(); }
    public void setNome(String nome) { this.nome.set(nome); }
    public StringProperty nomeProperty() { return nome; }

    public String getTipologia() { return tipologia.get(); }
    public void setTipologia(String tipologia) { this.tipologia.set(tipologia); }
    public StringProperty tipologiaProperty() { return tipologia; }

    public double getPrezzoVendita() { return prezzoVendita.get(); }
    public void setPrezzoVendita(double prezzo) { this.prezzoVendita.set(prezzo); }
    public DoubleProperty prezzoVenditaProperty() { return prezzoVendita; }

    public double getCostoRealizzazione() { return costoRealizzazione.get(); }
    public void setCostoRealizzazione(double costo) { this.costoRealizzazione.set(costo); }
    public DoubleProperty costoRealizzazioneProperty() { return costoRealizzazione; }

    public String getAllergeni() { return allergeni.get(); }
    public void setAllergeni(String allergeni) {
        this.allergeni.set(allergeni != null ? allergeni : "");
    }
    public StringProperty allergeniProperty() { return allergeni; }

    // === METODI CALCOLATI (NUOVI) ===

    public double getMargine() {
        return getPrezzoVendita() - getCostoRealizzazione();
    }

    public double getPercentualeMargine() {
        if (getPrezzoVendita() == 0) return 0;
        return (getMargine() / getPrezzoVendita()) * 100;
    }

    @Override
    public String toString() {
        return getNome() + " (" + getTipologia() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MenuProduct)) return false;
        MenuProduct other = (MenuProduct) obj;
        return getId() == other.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }
}