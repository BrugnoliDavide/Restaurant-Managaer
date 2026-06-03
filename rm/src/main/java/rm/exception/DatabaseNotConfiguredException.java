package rm.exception;

/**
 * Sollevata quando il database non è stato configurato.
 */
public class DatabaseNotConfiguredException extends RuntimeException {

    private final String userMessage;

    public DatabaseNotConfiguredException() {
        super("Database non configurato");
        this.userMessage = "Il database non è configurato. Verifica le impostazioni.";
    }

    public String getUserMessage() {
        return userMessage;
    }
}
