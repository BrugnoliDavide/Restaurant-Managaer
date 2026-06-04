package rm.exception;

import java.io.Serial;

/**
 * Eccezione sollevata quando il servizio di rilevamento allergeni
 * non trova alcun risultato per il nome del prodotto specificato.
 *
 * <p>Trattandosi di una condizione prevedibile e recuperabile dal chiamante,
 * è modellata come checked exception.</p>
 */
public class AllergenDetectionException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea una nuova eccezione con il messaggio indicato.
     *
     * @param message descrizione dell'errore
     */
    public AllergenDetectionException(String message) {
        super(message);
    }

    /**
     * Crea una nuova eccezione con messaggio e causa originaria.
     *
     * @param message descrizione dell'errore
     * @param cause   eccezione originaria
     */
    public AllergenDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}