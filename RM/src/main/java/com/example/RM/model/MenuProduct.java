package com.example.RM.model;

public class MenuProduct {

    // Attributi
    private int id;
    private String nome;
    private String tipologia;
    private double prezzoVendita;
    private double costoRealizzazione;
    private String allergeni;

    // --- COSTRUTTORE 1: COMPLETO (6 Argomenti) ---
    // Da usare in DatabaseService quando LEGGI un prodotto esistente
    public MenuProduct(int id, String nome, String tipologia, double prezzo, double costo, String allergeni) {
        // *** CORREZIONE QUI SOTTO ***
        this.id = id; // Salviamo l'ID vero che arriva dal Database!
        this.nome = nome;
        this.tipologia = tipologia;
        this.prezzoVendita = prezzo;
        this.costoRealizzazione = costo;
        this.allergeni = allergeni;
    }

    // --- COSTRUTTORE 2: PER NUOVI PRODOTTI (5 Argomenti) ---
    // Da usare quando l'utente crea un prodotto nuovo (l'ID non esiste ancora)
    public MenuProduct(String nome, String tipologia, double prezzo, double costo, String allergeni) {
        this.id = 0; // Qui va bene 0, perché il DB lo genererà dopo l'INSERT
        this.nome = nome;
        this.tipologia = tipologia;
        this.prezzoVendita = prezzo;
        this.costoRealizzazione = costo;
        this.allergeni = allergeni;
    }

    // --- COSTRUTTORE 3: SEMPLIFICATO (4 Argomenti) ---
    // Legacy / Test
    public MenuProduct(String nome, String tipologia, double prezzo, double costo) {
        this.id = 0;
        this.nome = nome;
        this.tipologia = tipologia;
        this.prezzoVendita = prezzo;
        this.costoRealizzazione = costo;
        this.allergeni = "";
    }

    // --- GETTER E SETTER ---
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getTipologia() { return tipologia; }
    public double getPrezzoVendita() { return prezzoVendita; }
    public double getCostoRealizzazione() { return costoRealizzazione; }
    public String getAllergeni() { return allergeni; }

    @Override
    public String toString() {
        return nome + " (" + tipologia + ")";
    }
}