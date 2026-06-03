package rm.dao;

import rm.printer.PrinterConfiguration;

/**
 * Data Access Object per la configurazione della stampante.
 * Gestisce il caricamento e salvataggio persistente delle preferenze di stampa.
 */
public interface PrinterConfigurationDAO {

    /**
     * Carica la configurazione della stampante.
     * Se non esiste, restituisce una configurazione di default.
     *
     * @return configurazione della stampante
     */
    PrinterConfiguration load();

    /**
     * Salva la configurazione della stampante.
     *
     * @param config configurazione da salvare
     * @return true se il salvataggio è riuscito, false altrimenti
     */
    boolean save(PrinterConfiguration config);

    /**
     * Elimina la configurazione salvata, ripristinando i valori di default.
     *
     * @return true se l'eliminazione è riuscita, false altrimenti
     */
    boolean reset();
}