package com.example.rm.app;

import com.example.rm.exception.InvalidUserSessionException;
import com.example.rm.model.User;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sessione utente corrente, implementata come Singleton thread-safe.
 *
 * <p>La thread-safety è garantita da due meccanismi combinati:
 * <ul>
 *   <li>{@code AtomicReference} su {@code instance}: assicura la pubblicazione
 *       sicura del riferimento tra thread;</li>
 *   <li>Blocco {@code synchronized} con doppia verifica (Double-Checked Locking):
 *       evita la creazione di istanze duplicate in caso di accesso concorrente.</li>
 * </ul>
 */
public class UserSession {

    private static final AtomicReference<UserSession> instance =
            new AtomicReference<>(null);

    private final User user;
    private final AtomicReference<Set<Integer>> managedTables =
            new AtomicReference<>(Set.of());

    private UserSession(User user) {
        this.user = user;
    }

    public static UserSession getInstance(User user) {
        if (instance.get() == null) {
            synchronized (UserSession.class) {
                if (instance.get() == null) {
                    instance.set(new UserSession(user));
                }
            }
        }
        return instance.get();
    }


    /**
     * Restituisce la sessione esistente.
     *
     * @throws InvalidUserSessionException se la sessione non è ancora stata creata
     */
    public static UserSession getInstance() {
        UserSession current = instance.get();
        if (current == null) {
            throw new InvalidUserSessionException();
        }
        return current;
    }

    /**
     * Distrugge la sessione corrente e pulisce la cache delle view.
     */
    public static void cleanUserSession() {
        instance.set(null);
        SceneManager.clearViewCache();
    }

    public User getUser() {
        return user;
    }

    public void setManagedTables(Set<Integer> tables) {
        managedTables.set(tables == null ? Set.of() : Set.copyOf(tables));
    }

    public boolean isTableManaged(int tableNumber) {
        Set<Integer> snapshot = managedTables.get();
        return snapshot.isEmpty() || snapshot.contains(tableNumber);
    }


    // quanto segue è esclusivo per le funzioni di test, non per la fase di produzione

    /**
     * Imposta un'istanza di test bypassando la logica di creazione normale.
     * <strong>Esclusivo per test unitari.</strong>
     *
     * @param testUser Utente fittizio da usare nei test
     */
    public static void setInstanceForTesting(User testUser) {
        instance.set(new UserSession(testUser));
    }


    /**
     * Azzera l'istanza.
     * <strong>Esclusivo per test unitari: da chiamare in @AfterEach.</strong>
     */
    public static void clearInstanceForTesting() {
        instance.set(null);
    }
}


