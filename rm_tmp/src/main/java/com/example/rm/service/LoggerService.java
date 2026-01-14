package com.example.rm.service;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servizio centralizzato per la gestione del logging nell'applicazione.
 * Elimina le dipendenze dirette tra controller.
 */
public final class LoggerService {

    private static final Logger APPLICATION_LOGGER =
            Logger.getLogger("com.example.rm");

    // Costruttore privato per utility class
    private LoggerService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Restituisce il logger centralizzato dell'applicazione
     */
    public static Logger getLogger() {
        return APPLICATION_LOGGER;
    }

    /**
     * Restituisce un logger specifico per una classe
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Metodi di convenienza per log comuni
     */
    public static void info(String message) {
        APPLICATION_LOGGER.log(Level.INFO, message);
    }

    public static void warning(String message) {
        APPLICATION_LOGGER.log(Level.WARNING, message);
    }

    public static void severe(String message) {
        APPLICATION_LOGGER.log(Level.SEVERE, message);
    }

    public static void severe(String message, Throwable thrown) {
        APPLICATION_LOGGER.log(Level.SEVERE, message, thrown);
    }
}