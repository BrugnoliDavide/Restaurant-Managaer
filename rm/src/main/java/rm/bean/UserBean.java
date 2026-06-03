package rm.bean;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Java Bean che rappresenta un utente/dipendente del sistema.
 * Corrisponde alla tabella {@code users} del database.
 *
 * <p><strong>Nota sulla sicurezza:</strong> il campo {@code passwordHash}
 * contiene l'hash BCrypt della password, mai la password in chiaro.
 * Non serializzare mai questo Bean attraverso canali non sicuri.</p>
 *
 * <p>I ruoli supportati dall'applicazione sono:
 * {@code cameriere}, {@code cucina}, {@code cassiere}, {@code manager}.</p>
 */
public class UserBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Costanti per i ruoli applicativi. */
    public static final String ROLE_WAITER  = "cameriere";
    public static final String ROLE_KITCHEN = "cucina";
    public static final String ROLE_CASHIER = "cassiere";
    public static final String ROLE_MANAGER = "manager";

    private String username;
    private String passwordHash;
    private String role;
    private String kitchenPreferences;   // JSON serializzato delle preferenze cucina (nullable)

    // ------------------------------------------------------------------ //
    //  Costruttori                                                         //
    // ------------------------------------------------------------------ //

    /** Costruttore no-arg richiesto dallo standard Java Bean. */
    public UserBean() {}

    /**
     * Costruttore di comodo senza campo {@code kitchenPreferences}.
     *
     * @param username      username univoco dell'utente
     * @param passwordHash  hash BCrypt della password
     * @param role          ruolo applicativo
     */
    public UserBean(String username, String passwordHash, String role) {
        this.username      = username;
        this.passwordHash  = passwordHash;
        this.role          = role;
    }

    /**
     * Costruttore completo.
     *
     * @param username             username univoco dell'utente
     * @param passwordHash         hash BCrypt della password
     * @param role                 ruolo applicativo
     * @param kitchenPreferences   preferenze cucina in formato JSON (può essere {@code null})
     */
    public UserBean(String username, String passwordHash,
                    String role, String kitchenPreferences) {
        this.username            = username;
        this.passwordHash        = passwordHash;
        this.role                = role;
        this.kitchenPreferences  = kitchenPreferences;
    }

    // ------------------------------------------------------------------ //
    //  Getter e Setter                                                     //
    // ------------------------------------------------------------------ //

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getKitchenPreferences() {
        return kitchenPreferences;
    }

    public void setKitchenPreferences(String kitchenPreferences) {
        this.kitchenPreferences = kitchenPreferences;
    }

    // ------------------------------------------------------------------ //
    //  Metodi di utilità                                                   //
    // ------------------------------------------------------------------ //

    /** @return {@code true} se il ruolo è {@value #ROLE_MANAGER} */
    public boolean isManager() {
        return ROLE_MANAGER.equals(role);
    }

    /** @return {@code true} se il ruolo è {@value #ROLE_KITCHEN} */
    public boolean isKitchen() {
        return ROLE_KITCHEN.equals(role);
    }

    /** @return {@code true} se il ruolo è {@value #ROLE_WAITER} */
    public boolean isWaiter() {
        return ROLE_WAITER.equals(role);
    }

    /** @return {@code true} se il ruolo è {@value #ROLE_CASHIER} */
    public boolean isCashier() {
        return ROLE_CASHIER.equals(role);
    }

    // ------------------------------------------------------------------ //
    //  equals, hashCode, toString                                          //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBean other)) return false;
        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    /**
     * Il campo {@code passwordHash} è deliberatamente omesso
     * dalla rappresentazione testuale per ragioni di sicurezza.
     */
    @Override
    public String toString() {
        return String.format("UserBean{username='%s', role='%s'}", username, role);
    }
}