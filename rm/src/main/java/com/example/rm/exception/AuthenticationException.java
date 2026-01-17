package com.example.rm.exception;

/**
 * Errore durante l'autenticazione dell'utente.
 * Il messaggio è pensato per essere mostrato all'utente finale.
 */
public class AuthenticationException extends RuntimeException {

    private final String userMessage;

    public AuthenticationException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public AuthenticationException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
