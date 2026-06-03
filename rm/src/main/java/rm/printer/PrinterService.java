package rm.printer;

import rm.dao.PrinterConfigurationDAO;
import rm.dao.impl.PreferencesPrinterConfigurationDAO;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import rm.model.Order;
import rm.model.OrderItem;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    qui ho usato Java preference e non DAO in quanto è configurazione globale: La stampante è una configurazione di sistema, non per utente
    e soprattutto non modifica il DB
 */


/**
 * Servizio singleton per la gestione della stampa comande su stampante termica ESC/POS.
 *
 * <p>Thread-safety garantita tramite:
 * <ul>
 *   <li>Initialization-on-Demand Holder Idiom per il Singleton (fix SonarCloud S2168);</li>
 *   <li>{@code AtomicReference} per i campi oggetto {@code config} ed {@code escPosPrinter},
 *       in conformità con la regola SonarCloud S3077 ("Use a thread-safe type").</li>
 * </ul>
 */
public class PrinterService {

    private static final Logger LOGGER = Logger.getLogger(PrinterService.class.getName());

    // -----------------------------------------------------------------------
    // Singleton — Initialization-on-Demand Holder (S2168 fix)
    // La JVM garantisce che l'inizializzazione di una classe sia thread-safe
    // (JLS §12.4.2), eliminando la necessità di volatile o synchronized.
    // -----------------------------------------------------------------------
    private static final class Holder {
        private static final PrinterService INSTANCE = new PrinterService();

        private Holder() {
            // utility holder — non istanziabile
        }
    }

    public static PrinterService getInstance() {
        return Holder.INSTANCE;
    }

    // -----------------------------------------------------------------------
    // Campi di istanza — AtomicReference su oggetti mutabili (S3077 fix)
    // volatile garantisce solo la visibilità del riferimento, non l'atomicità
    // delle operazioni composte; AtomicReference risolve entrambi i problemi.
    // -----------------------------------------------------------------------
    private final AtomicReference<PrinterConfiguration> config  = new AtomicReference<>(null);
    private final AtomicReference<PrintService>         escPosPrinter = new AtomicReference<>(null);

    private final ExecutorService printExecutor;

    // -----------------------------------------------------------------------
    // Costruttore privato
    // -----------------------------------------------------------------------
    private PrinterService() {
        this.printExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PrinterService-Thread");
            t.setDaemon(true);
            return t;
        });
        loadConfiguration();
        initializePrinter();
    }

    // -----------------------------------------------------------------------
    // Inizializzazione
    // -----------------------------------------------------------------------

    /**
     * Carica la configurazione della stampante dal DAO.
     */
    private void loadConfiguration() {
        PrinterConfigurationDAO dao = new PreferencesPrinterConfigurationDAO();
        PrinterConfiguration loaded = dao.load();

        if (loaded == null) {
            // Configurazione di default
            loaded = new PrinterConfiguration();
            loaded.setPrinterName("Auto-detect");
            loaded.setEnabled(false);
            loaded.setPrintWidth(48);
        }
        config.set(loaded);
    }

    /**
     * Inizializza la connessione alla stampante.
     */
    private void initializePrinter() {
        PrinterConfiguration cfg = config.get();

        if (cfg == null || !cfg.isEnabled()) {
            LOGGER.info("Stampa comande disabilitata dalla configurazione");
            return;
        }

        try {
            PrintService found = "Auto-detect".equals(cfg.getPrinterName())
                    ? findEscPosPrinter()
                    : findPrinterByName(cfg.getPrinterName());

            escPosPrinter.set(found);

            if (found != null) {
                // S2629 fix: nessuna concatenazione di stringa nel logger
                LOGGER.log(Level.INFO, "Stampante inizializzata: {0}", found.getName());
            } else {
                LOGGER.warning("Nessuna stampante ESC/POS trovata");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore inizializzazione stampante", e);
        }
    }

    // -----------------------------------------------------------------------
    // API pubblica
    // -----------------------------------------------------------------------

    /**
     * Stampa una comanda in modo asincrono.
     * Non blocca il thread chiamante.
     */
    public void printOrderAsync(Order order, List<OrderItem> items) {
        PrinterConfiguration cfg     = config.get();
        PrintService          printer = escPosPrinter.get();

        if (cfg == null || !cfg.isEnabled() || printer == null) {
            LOGGER.fine("Stampa saltata - stampante non disponibile");
            return;
        }

        printExecutor.submit(() -> {
            try {
                printOrderSync(order, items);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                        "Errore stampa comanda #{0}", order.getId());
            }
        });
    }

    /**
     * Stampa una comanda in modo sincrono.
     * Utilizzare solo per test o dove necessario bloccare l'esecuzione.
     */
    public void printOrderSync(Order order, List<OrderItem> items)
            throws PrinterException {

        PrintService printer = escPosPrinter.get();
        if (printer == null) {
            throw new PrinterException("Stampante non inizializzata");
        }

        try (PrinterOutputStream printerOutputStream =
                     new PrinterOutputStream(printer);
             EscPos escpos = new EscPos(printerOutputStream)) {

            formatAndPrint(escpos, order, items);

            escpos.feed(3);
            escpos.cut(EscPos.CutMode.PART);

        } catch (IOException e) {
            throw new PrinterException("Errore I/O durante la stampa", e);
        }
    }

    /**
     * Aggiorna la configurazione e reinizializza la stampante.
     *
     * @param newConfig nuova configurazione (non deve essere {@code null})
     */
    public void updateConfiguration(PrinterConfiguration newConfig) {
        config.set(newConfig);
        PrinterConfigurationDAO dao = new PreferencesPrinterConfigurationDAO();
        dao.save(newConfig);
        initializePrinter();
    }

    /**
     * Restituisce la configurazione corrente.
     */
    public PrinterConfiguration getConfiguration() {
        return config.get();
    }

    /**
     * Elenca tutte le stampanti disponibili sul sistema.
     */
    public String[] getAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        String[] names = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            names[i] = services[i].getName();
        }
        return names;
    }

    /**
     * Shutdown del servizio - chiamare alla chiusura dell'applicazione.
     */
    public void shutdown() {
        printExecutor.shutdown();
    }

    // -----------------------------------------------------------------------
    // Metodi privati di utilità
    // -----------------------------------------------------------------------

    /**
     * Formattazione e stampa del contenuto della comanda.
     */
    private void formatAndPrint(EscPos escpos, Order order, List<OrderItem> items)
            throws IOException {

        Style titleStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setBold(true)
                .setJustification(EscPosConst.Justification.Center);

        Style headerStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setBold(true);

        Style normalStyle = new Style();

        // Intestazione
        escpos.write(titleStyle, "COMANDA");
        escpos.feed(1);

        escpos.write(headerStyle, "Ordine #" + order.getId());
        escpos.feed(1);

        // Informazioni ordine
        escpos.writeLF(normalStyle, "Tavolo: " + order.getTavolo());
        escpos.writeLF(normalStyle,
                "Ora: " + order.getDataOra().format(
                        DateTimeFormatter.ofPattern("HH:mm")));
        escpos.writeLF(normalStyle, "Cameriere: " + order.getUsername());

        if (order.getNote() != null && !order.getNote().isEmpty()) {
            escpos.writeLF(normalStyle, "Note: " + order.getNote());
        }

        escpos.feed(1);
        escpos.writeLF("----------------------------------------");

        // Articoli
        for (OrderItem item : items) {
            String line = String.format("%dx %-30s",
                    item.getQuantita(),
                    truncate(item.getNomeSnapshot(), 30));
            escpos.writeLF(normalStyle, line);
        }

        escpos.writeLF("----------------------------------------");
    }

    /**
     * Tronca una stringa alla lunghezza specificata.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }

    /**
     * Cerca automaticamente una stampante ESC/POS.
     */
    private PrintService findEscPosPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService service : services) {
            String name = service.getName().toLowerCase();
            if (name.contains("pos")     || name.contains("thermal") ||
                    name.contains("receipt") || name.contains("tm-")     ||
                    name.contains("star")    || name.contains("epson")) {
                return service;
            }
        }

        // Fallback: prima stampante disponibile
        return services.length > 0 ? services[0] : null;
    }

    /**
     * Cerca una stampante per nome esatto.
     */
    private PrintService findPrinterByName(String name) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(name)) {
                return service;
            }
        }
        return null;
    }
}