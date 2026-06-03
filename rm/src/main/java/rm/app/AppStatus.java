package rm.app;

/**
 * Stato globale dell'applicazione.
 * Il campo è dichiarato volatile per garantire la visibilità delle scritture
 * tra thread distinti senza ricorrere a sincronizazione esplicita:
 * il flag viene scritto una sola volta all'avvio e letto
 * succesivamente da altri thread, quindi volatile è sufficiente per evitare race condition
 */
public final class AppStatus {

    // volatile garantisce che non vi siano race-condition.
    private static volatile boolean dbConnectionOk = false;

    private AppStatus() {}

    public static boolean isDbConnectionOk() {
        return dbConnectionOk;
    }

    public static void setDbConnectionOk(boolean ok) {
        dbConnectionOk = ok;
    }
}