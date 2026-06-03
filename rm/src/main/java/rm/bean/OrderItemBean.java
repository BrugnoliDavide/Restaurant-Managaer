package rm.bean;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Java Bean che rappresenta una singola riga (articolo) di un ordine.
 * Corrisponde alla tabella {@code order_items} del database.
 *
 * <p>I campi {@code *Snapshot} conservano i valori di prezzo e nome
 * al momento dell'ordine, in modo che eventuali moFdifiche successive
 * al menù non alterino lo storico degli ordini già registrati.</p>
 */
public class OrderItemBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int    orderId;
    private int    menuItemId;
    private int    quantita;
    private BigDecimal prezzoVenditaSnapshot = BigDecimal.ZERO;
    private BigDecimal costoRealizzazioneSnapshot = BigDecimal.ZERO;
    private String nomeProdottoSnapshot;

    // ------------------------------------------------------------------ //
    //  Costruttori                                                         //
    // ------------------------------------------------------------------ //

    /** Costruttore no-arg richiesto dallo standard Java Bean. */
    public OrderItemBean() {}

    /**
     * Costruttore di comodo per inizializzare tutti i campi.
     *
     * @param orderId                      ID dell'ordine padre
     * @param menuItemId                   ID del prodotto nel menù
     * @param quantita                     quantità ordinata
     * @param prezzoVenditaSnapshot        prezzo unitario al momento dell'ordine
     * @param costoRealizzazioneSnapshot   costo unitario al momento dell'ordine
     * @param nomeProdottoSnapshot         nome del prodotto al momento dell'ordine
     */
    public OrderItemBean(int orderId, int menuItemId, int quantita,
                         BigDecimal prezzoVenditaSnapshot,
                         BigDecimal costoRealizzazioneSnapshot,
                         String nomeProdottoSnapshot) {
        this.orderId                     = orderId;
        this.menuItemId                  = menuItemId;
        this.quantita                    = quantita;
        this.prezzoVenditaSnapshot       = prezzoVenditaSnapshot;
        this.costoRealizzazioneSnapshot  = costoRealizzazioneSnapshot;
        this.nomeProdottoSnapshot        = nomeProdottoSnapshot != null ? nomeProdottoSnapshot : "";
    }

    // ------------------------------------------------------------------ //
    //  Getter e Setter                                                     //
    // ------------------------------------------------------------------ //

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }

    public BigDecimal getPrezzoVenditaSnapshot() {
        return prezzoVenditaSnapshot;
    }

    public void setPrezzoVenditaSnapshot(BigDecimal prezzoVenditaSnapshot) {
        this.prezzoVenditaSnapshot = prezzoVenditaSnapshot;
    }

    public BigDecimal getCostoRealizzazioneSnapshot() {
        return costoRealizzazioneSnapshot;
    }

    public void setCostoRealizzazioneSnapshot(BigDecimal costoRealizzazioneSnapshot) {
        this.costoRealizzazioneSnapshot = costoRealizzazioneSnapshot;
    }

    public String getNomeProdottoSnapshot() {
        return nomeProdottoSnapshot;
    }

    public void setNomeProdottoSnapshot(String nomeProdottoSnapshot) {
        this.nomeProdottoSnapshot = nomeProdottoSnapshot != null ? nomeProdottoSnapshot : "";
    }

    // ------------------------------------------------------------------ //
    //  Metodi di utilità                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Calcola il subtotale della riga (quantità × prezzo unitario snapshot).
     *
     * @return subtotale in euro
     */
    public BigDecimal getSubtotale() {
        return prezzoVenditaSnapshot.multiply(BigDecimal.valueOf(quantita));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemBean other)) return false;
        return orderId == other.orderId && menuItemId == other.menuItemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, menuItemId);
    }

    @Override
    public String toString() {
        return String.format("OrderItemBean{orderId=%d, menuItemId=%d, nome='%s', quantita=%d, subtotale=%.2f}",
                orderId, menuItemId, nomeProdottoSnapshot, quantita, getSubtotale());
    }
}