package com.example.rm.model;

import javafx.beans.property.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MenuProduct implements Serializable {

    private static final long serialVersionUID = 1L;

   private transient IntegerProperty id;
    private transient StringProperty nome;
    private transient StringProperty tipologia;
    private transient DoubleProperty prezzoVendita;
    private transient DoubleProperty costoRealizzazione;
    private transient StringProperty allergeni;

    public MenuProduct() {
        this.id = new SimpleIntegerProperty(0);
        this.nome = new SimpleStringProperty("");
        this.tipologia = new SimpleStringProperty("");
        this.prezzoVendita = new SimpleDoubleProperty(0.0);
        this.costoRealizzazione = new SimpleDoubleProperty(0.0);
        this.allergeni = new SimpleStringProperty("");
    }

    public MenuProduct(int id, String nome, String tipologia,
                       double prezzo, double costo, String allergeni) {
        this.id = new SimpleIntegerProperty(id);
        this.nome = new SimpleStringProperty(nome);
        this.tipologia = new SimpleStringProperty(tipologia);
        this.prezzoVendita = new SimpleDoubleProperty(prezzo);
        this.costoRealizzazione = new SimpleDoubleProperty(costo);
        this.allergeni = new SimpleStringProperty(allergeni != null ? allergeni : "");
    }

    // Senza ID
    public MenuProduct(String nome, String tipologia,
                       double prezzo, double costo, String allergeni) {
        this(0, nome, tipologia, prezzo, costo, allergeni);
    }

    // Legacy
    public MenuProduct(String nome, String tipologia, double prezzo, double costo) {
        this(0, nome, tipologia, prezzo, costo, "");
    }


    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(getId());
        s.writeUTF(getNome() != null ? getNome() : "");
        s.writeUTF(getTipologia() != null ? getTipologia() : "");
        s.writeDouble(getPrezzoVendita());
        s.writeDouble(getCostoRealizzazione());
        s.writeUTF(getAllergeni() != null ? getAllergeni() : "");
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        // Ricostruiamo le Property JavaFX usando i valori letti
        this.id = new SimpleIntegerProperty(s.readInt());
        this.nome = new SimpleStringProperty(s.readUTF());
        this.tipologia = new SimpleStringProperty(s.readUTF());
        this.prezzoVendita = new SimpleDoubleProperty(s.readDouble());
        this.costoRealizzazione = new SimpleDoubleProperty(s.readDouble());
        this.allergeni = new SimpleStringProperty(s.readUTF());
    }


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

    public double getCostoRealizzazione() { return costoRealizzazione.get(); }

    public String getAllergeni() { return allergeni.get(); }
    public void setAllergeni(String allergeni) {
        this.allergeni.set(allergeni != null ? allergeni : "");
    }

    public double getMargine() {
        return getPrezzoVendita() - getCostoRealizzazione();
    }

    public double getPercentualeMargine() {
        if (getPrezzoVendita() == 0) return 0;
        return (getMargine() / getPrezzoVendita()) * 100;
    }

    public void setPrezzoVendita(double prezzo) {  this.prezzoVendita.set(prezzo); }
    public void setCostoRealizzazione(double costo){this.costoRealizzazione.set(costo);}


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