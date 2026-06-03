package rm.service;

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

    public static final String COL_DATA_ORA = "data_ora";
    public static final String COL_TAVOLO = "tavolo";
    public static final String COL_NOTE = "note";
    public static final String COL_STATUS = "status";
    public static final String COL_TOTALE_CALCOLATO = "totale_calcolato";


   public static final String POSTGRES_PREFIX = "jdbc:postgresql://";
}