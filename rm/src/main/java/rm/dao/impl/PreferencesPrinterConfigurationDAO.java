package rm.dao.impl;

import rm.dao.PrinterConfigurationDAO;
import rm.printer.PrinterConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Implementazione di PrinterConfigurationDAO che usa Java Preferences API
 * per persistere la configurazione della stampante.
 *
 * La configurazione viene salvata a livello di sistema e persiste tra
 * le esecuzioni dell'applicazione.
 */
public class PreferencesPrinterConfigurationDAO implements PrinterConfigurationDAO {

    private static final Logger LOGGER = Logger.getLogger(
            PreferencesPrinterConfigurationDAO.class.getName());

    // Nodo delle preferenze per la stampante
    private static final String PREFS_NODE = "com/example/rm/printer";

    // Chiavi per i singoli campi
    private static final String KEY_PRINTER_NAME = "printerName";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PRINT_WIDTH = "printWidth";
    private static final String KEY_AUTO_CUT = "autoCut";
    private static final String KEY_FEED_LINES = "feedLines";

    // Valori di default
    private static final String DEFAULT_PRINTER_NAME = "Auto-detect";
    private static final boolean DEFAULT_ENABLED = false;
    private static final int DEFAULT_PRINT_WIDTH = 48;
    private static final boolean DEFAULT_AUTO_CUT = true;
    private static final int DEFAULT_FEED_LINES = 3;

    private final Preferences prefs;

    public PreferencesPrinterConfigurationDAO() {
        this.prefs = Preferences.userRoot().node(PREFS_NODE);
    }

    @Override
    public PrinterConfiguration load() {
        try {
            PrinterConfiguration config = new PrinterConfiguration();

            // Carica ogni campo dalle preferences
            config.setPrinterName(prefs.get(KEY_PRINTER_NAME, DEFAULT_PRINTER_NAME));
            config.setEnabled(prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED));
            config.setPrintWidth(prefs.getInt(KEY_PRINT_WIDTH, DEFAULT_PRINT_WIDTH));
            config.setAutoCut(prefs.getBoolean(KEY_AUTO_CUT, DEFAULT_AUTO_CUT));
            config.setFeedLines(prefs.getInt(KEY_FEED_LINES, DEFAULT_FEED_LINES));

            LOGGER.fine("Configurazione stampante caricata con successo");
            return config;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Errore caricamento configurazione stampante, uso default", e);
            return getDefaultConfiguration();
        }
    }

    @Override
    public boolean save(PrinterConfiguration config) {
        if (config == null) {
            LOGGER.warning("Tentativo di salvare configurazione null");
            return false;
        }

        try {
            // Salva ogni campo nelle preferences
            prefs.put(KEY_PRINTER_NAME, config.getPrinterName());
            prefs.putBoolean(KEY_ENABLED, config.isEnabled());
            prefs.putInt(KEY_PRINT_WIDTH, config.getPrintWidth());
            prefs.putBoolean(KEY_AUTO_CUT, config.isAutoCut());
            prefs.putInt(KEY_FEED_LINES, config.getFeedLines());

            // Forza il flush su disco
            prefs.flush();

            LOGGER.info("Configurazione stampante salvata con successo");
            return true;

        } catch (BackingStoreException e) {
            LOGGER.log(Level.SEVERE,
                    "Errore salvataggio configurazione stampante", e);
            return false;
        }
    }

    @Override
    public boolean reset() {
        try {
            // Rimuove tutte le chiavi del nodo
            prefs.clear();
            prefs.flush();

            LOGGER.info("Configurazione stampante resettata ai valori di default");
            return true;

        } catch (BackingStoreException e) {
            LOGGER.log(Level.SEVERE,
                    "Errore reset configurazione stampante", e);
            return false;
        }
    }

    /**
     * Restituisce una configurazione con valori di default.
     */
    private PrinterConfiguration getDefaultConfiguration() {
        PrinterConfiguration config = new PrinterConfiguration();
        config.setPrinterName(DEFAULT_PRINTER_NAME);
        config.setEnabled(DEFAULT_ENABLED);
        config.setPrintWidth(DEFAULT_PRINT_WIDTH);
        config.setAutoCut(DEFAULT_AUTO_CUT);
        config.setFeedLines(DEFAULT_FEED_LINES);
        return config;
    }
}