package com.example.rm.model;

import javafx.beans.property.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MenuProduct implements Serializable {

    private static final long serialVersionUID = 1L;

   private transient IntegerProperty id;
    private transient StringProperty nome;
    private transient StringProperty tipologia;
    private transient ObjectProperty<BigDecimal> prezzoVendita;
    private transient ObjectProperty<BigDecimal> costoRealizzazione;
    private transient StringProperty allergeni;

    public MenuProduct() {
        this.id = new SimpleIntegerProperty(0);
        this.nome = new SimpleStringProperty("");
        this.tipologia = new SimpleStringProperty("");
        this.prezzoVendita = new SimpleObjectProperty<>(BigDecimal.ZERO);
        this.costoRealizzazione = new SimpleObjectProperty<>(BigDecimal.ZERO);
        this.allergeni = new SimpleStringProperty("");
    }

    public MenuProduct(int id, String nome, String tipologia,
                       BigDecimal prezzo, BigDecimal costo, String allergeni) {
        this.id = new SimpleIntegerProperty(id);
        this.nome = new SimpleStringProperty(nome);
        this.tipologia = new SimpleStringProperty(tipologia);
        this.prezzoVendita = new SimpleObjectProperty<>(prezzo);
        this.costoRealizzazione = new SimpleObjectProperty<>(costo);
        this.allergeni = new SimpleStringProperty(allergeni != null ? allergeni : "");
    }

    // Senza ID
    public MenuProduct(String nome, String tipologia,
                       BigDecimal prezzo, BigDecimal costo, String allergeni) {
        this(0, nome, tipologia, prezzo, costo, allergeni);
    }

    // Legacy
    public MenuProduct(String nome, String tipologia, BigDecimal prezzo, BigDecimal costo) {
        this(0, nome, tipologia, prezzo, costo, "");
    }


    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(getId());
        s.writeUTF(getNome() != null ? getNome() : "");
        s.writeUTF(getTipologia() != null ? getTipologia() : "");
        s.writeUTF(getPrezzoVendita().toPlainString());
        s.writeUTF(getCostoRealizzazione().toPlainString());
        s.writeUTF(getAllergeni() != null ? getAllergeni() : "");
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        // Ricostruiamo le Property JavaFX usando i valori letti
        this.id = new SimpleIntegerProperty(s.readInt());
        this.nome = new SimpleStringProperty(s.readUTF());
        this.tipologia = new SimpleStringProperty(s.readUTF());
        this.prezzoVendita = new SimpleObjectProperty<>(new BigDecimal(s.readUTF()));
        this.costoRealizzazione = new SimpleObjectProperty<>(new BigDecimal(s.readUTF()));
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

    public BigDecimal getPrezzoVendita() { return prezzoVendita.get(); }

    public BigDecimal getCostoRealizzazione() { return costoRealizzazione.get(); }

    public String getAllergeni() { return allergeni.get(); }
    public void setAllergeni(String allergeni) {
        this.allergeni.set(allergeni != null ? allergeni : "");
    }

    public BigDecimal getMargine() {
        return getPrezzoVendita().subtract(getCostoRealizzazione());
    }

    public int getPercentualeMargine() {
        if (getPrezzoVendita().compareTo(BigDecimal.ZERO) == 0) return 0;
        return getMargine()
                .multiply(BigDecimal.valueOf(100))
                .divide(getPrezzoVendita(), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public void setPrezzoVendita(BigDecimal prezzo) {  this.prezzoVendita.set(prezzo); }
    public void setCostoRealizzazione(BigDecimal costo){this.costoRealizzazione.set(costo);}


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