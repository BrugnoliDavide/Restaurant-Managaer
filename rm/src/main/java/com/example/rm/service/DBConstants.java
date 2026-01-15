package com.example.rm.service;

/**
 * Costanti per i nomi delle colonne del database
 * Evita errori di battitura e facilita la manutenzione
 */
public final class DBConstants {

    private DBConstants() {
        throw new IllegalStateException("Utility class - non istanziabile");
    }


    public static final String COL_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_ROLE = "role";
    public static final String COL_PASSWORD = "password";

    public static final String COL_NOME = "nome";
    public static final String COL_TIPOLOGIA = "tipologia";
    public static final String COL_PREZZO_VENDITA = "prezzo_vendita";
    public static final String COL_COSTO_REALIZZAZIONE = "costo_realizzazione";
    public static final String COL_ALLERGENI = "allergeni";

   public static final String COL_DATA_ORA = "data_ora";
    public static final String COL_TAVOLO = "tavolo";
    public static final String COL_NOTE = "note";
    public static final String COL_STATUS = "status";
    public static final String COL_TOTALE_CALCOLATO = "totale_calcolato";

   public static final String COL_ORDER_ID = "order_id";
    public static final String COL_MENU_ITEM_ID = "menu_item_id";
    public static final String COL_QUANTITA = "quantita";
    public static final String COL_PREZZO_SNAPSHOT = "prezzo_vendita_snapshot";
    public static final String COL_COSTO_SNAPSHOT = "costo_realizzazione_snapshot";

    public static final String STATUS_TODO = "to-do";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_CLOSED = "closed";

   public static final String POSTGRES_PREFIX = "jdbc:postgresql://";
}