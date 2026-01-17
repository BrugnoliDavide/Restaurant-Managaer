package com.example.rm.exception;

/**
 * Sollevata quando una View richiesta non esiste o non è supportata.
 */
public class ViewNotFoundException extends RuntimeException {

    public ViewNotFoundException(String viewName) {
        super("Vista non disponibile: " + viewName);
    }

    public ViewNotFoundException(String viewName, Throwable cause) {
        super("Errore nel caricamento della vista: " + viewName, cause);
    }
}
