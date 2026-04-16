package com.example.rm.app;

import com.example.rm.exception.InvalidUserSessionException;
import com.example.rm.model.User;

import java.util.Set;

/**
 * Sessione utente corrente, implementata come Singleton thread-safe.
 *
 * <p>La thread-safety è garantita da due meccanismi combinati:
 * <ul>
 *   <li>{@code volatile} su {@code instance}: assicura che la scrittura del
 *       riferimento sia visibile a tutti i thread immediatamente dopo il
 *       completamento del costruttore (no partially-constructed object).</li>
 *   <li>Blocco {@code synchronized} con doppia verifica (Double-Checked Locking):
 *       evita la creazione di istanze duplicate in caso di accesso concorrente
 *       al primo {@code getInstance(User)}.</li>
 * </ul>
 */
public class UserSession {

    // volatile è indispensabile per il corretto funzionamento del DCL pattern.
    // Senza volatile il compilatore/JIT potrebbe riordinare le istruzioni
    // e rendere visibile il riferimento prima che il costruttore sia completato.
    private static volatile UserSession instance;

    private final User user;
    private volatile Set<Integer> managedTables = Set.of();

    private UserSession(User user) {
        this.user = user;
    }

    /**
     * Restituisce l'istanza corrente, creandola se non ancora esistente.
     * Sicuro per accesso concorrente grazie al pattern Double-Checked Locking.
     *
     * @param user Utente da associare alla sessione (usato solo alla prima chiamata)
     * @return l'istanza singleton di UserSession
     */
    public static UserSession getInstance(User user) {
        // Prima verifica (senza lock): evita la sincronizzazione quando
        // l'istanza esiste già, riducendo la contesa.
        if (instance == null) {
            synchronized (UserSession.class) {
                // Seconda verifica (con lock): necessaria perché tra la prima
                // verifica e l'acquisizione del lock un altro thread potrebbe
                // aver già creato l'istanza.
                if (instance == null) {
                    instance = new UserSession(user);
                }
            }
        }
        return instance;
    }

    /**
     * Restituisce la sessione esistente.
     *
     * @throws InvalidUserSessionException se la sessione non è ancora stata creata
     */
    public static UserSession getInstance() {
        UserSession current = instance; // lettura volatile una sola volta
        if (current == null) {
            throw new InvalidUserSessionException();
        }
        return current;
    }

    /**
     * Distrugge la sessione corrente e pulisce la cache delle view.
     */
    public static void cleanUserSession() {
        instance = null;
        SceneManager.clearViewCache();
    }

    public User getUser() {
        return user;
    }

    public void setManagedTables(Set<Integer> tables) {
        this.managedTables = (tables == null) ? Set.of() : Set.copyOf(tables);
    }

    public boolean isTableManaged(int tableNumber) {
        Set<Integer> snapshot = managedTables;
        return snapshot.isEmpty() || snapshot.contains(tableNumber);
    }

    // -------------------------------------------------------------------------
    // Metodi esclusivi per i test — NON utilizzati in produzione.
    // -------------------------------------------------------------------------

    /**
     * Imposta un'istanza di test bypassando la logica di creazione normale.
     * <strong>Esclusivo per test unitari.</strong>
     *
     * @param testUser Utente fittizio da usare nei test
     */
    public static void setInstanceForTesting(User testUser) {
        instance = new UserSession(testUser);
    }

    /**
     * Azzera l'istanza.
     * <strong>Esclusivo per test unitari: da chiamare in @AfterEach.</strong>
     */
    public static void clearInstanceForTesting() {
        instance = null;
    }
}