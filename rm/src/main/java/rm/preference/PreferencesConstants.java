package rm.preference;


public final class PreferencesConstants {

    private PreferencesConstants() {
        // Non istanziabile
    }

    // Valore speciale per "Altro", tale variabile alla fine non è stata implementata
    // resta comunque a disposizione per possibili implementazioni
    public static final String CATEGORY_OTHER = "ALTRO";

    public static final boolean DEFAULT_SPLIT_ORDERS = false;
    public static final boolean DEFAULT_INCLUDE_OTHER = false;
}
