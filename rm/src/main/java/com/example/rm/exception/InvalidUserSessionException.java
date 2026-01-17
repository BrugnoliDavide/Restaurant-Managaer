package com.example.rm.exception;

/**
 * Indica una sessione utente non valida o assente.
 */
public class InvalidUserSessionException extends RuntimeException {

    public InvalidUserSessionException() {
        super("Sessione utente non valida o scaduta");
    }
}
