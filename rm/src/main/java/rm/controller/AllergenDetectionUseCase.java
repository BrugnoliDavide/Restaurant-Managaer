package rm.controller;

import rm.exception.AllergenDetectionException;

/**
 * Contratto per il servizio di rilevamento automatico degli allergeni.
 *
 * <p>Le implementazioni interrogano una sorgente esterna (ad es. Spoonacular API)
 * per ottenere la lista degli allergeni associati a un prodotto alimentare
 * identificato dal nome.</p>
 */
public interface AllergenDetectionUseCase {

    /**
     * Rileva gli allergeni associati al prodotto alimentare indicato.
     *
     * @param foodName nome del piatto o ingrediente da cercare (non {@code null}, non vuoto)
     * @return stringa con gli allergeni separati da virgola (es. {@code "Glutine, Lattosio"});
     *         mai {@code null}, mai vuota
     * @throws AllergenDetectionException se la sorgente non restituisce alcun risultato
     *                                    per il nome fornito
     */
    String detectAllergens(String foodName) throws AllergenDetectionException;
}