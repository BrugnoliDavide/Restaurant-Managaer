package com.example.rm.printer;

import com.example.rm.dao.PrinterConfigurationDAO;
import com.example.rm.dao.impl.PreferencesPrinterConfigurationDAO;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    qui ho usato Java preference e non DAO in quanto è configurazione globale: La stampante è una configurazione di sistema, non per utente
    e sospratutto non modifica il DB
 */


/**
 * Servizio singleton per la gestione della stampa comande su stampante termica ESC/POS.
 * Thread-safe e con supporto per stampa asincrona.
 */
public class PrinterService {
    private static final    Logger LOGGER = Logger.getLogger(PrinterService.class.getName());
    private static volatile PrinterService instance;
    private static final    Object lock = new Object();

    private PrinterConfiguration config;
    private final ExecutorService printExecutor;
    private PrintService escPosPrinter;

    private PrinterService() {
        this.printExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PrinterService-Thread");
            t.setDaemon(true);
            return t;
        });
        loadConfiguration();
        initializePrinter();
    }

    public static PrinterService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new PrinterService();
                }
            }
        }
        return instance;
    }

    /**
     * Carica la configurazione della stampante dal DAO.
     */
    private void loadConfiguration() {
        PrinterConfigurationDAO dao = new PreferencesPrinterConfigurationDAO();
        this.config = dao.load();

        if (this.config == null) {
            // Configurazione di default
            this.config = new PrinterConfiguration();
            this.config.setPrinterName("Auto-detect");
            this.config.setEnabled(false);
            this.config.setPrintWidth(48);
        }
    }

    /**
     * Inizializza la connessione alla stampante.
     */
    private void initializePrinter() {
        if (!config.isEnabled()) {
            LOGGER.info("Stampa comande disabilitata dalla configurazione");
            return;
        }

        try {
            if ("Auto-detect".equals(config.getPrinterName())) {
                escPosPrinter = findEscPosPrinter();
            } else {
                escPosPrinter = findPrinterByName(config.getPrinterName());
            }

            if (escPosPrinter != null) {
                LOGGER.info("Stampante inizializzata: " + escPosPrinter.getName());
            } else {
                LOGGER.warning("Nessuna stampante ESC/POS trovata");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore inizializzazione stampante", e);
        }
    }

    /**
     * Stampa una comanda in modo asincrono.
     * Non blocca il thread chiamante.
     */
    public void printOrderAsync(Order order, List<OrderItem> items) {
        if (!config.isEnabled() || escPosPrinter == null) {
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

        if (escPosPrinter == null) {
            throw new PrinterException("Stampante non inizializzata");
        }

        try (PrinterOutputStream printerOutputStream =
                     new PrinterOutputStream(escPosPrinter);
             EscPos escpos = new EscPos(printerOutputStream)) {

            formatAndPrint(escpos, order, items);

            escpos.feed(3);
            escpos.cut(EscPos.CutMode.PART);

        } catch (IOException e) {
            throw new PrinterException("Errore I/O durante la stampa", e);
        }
    }

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
            // Cerca pattern comuni per stampanti termiche
            if (name.contains("pos") || name.contains("thermal") ||
                    name.contains("receipt") || name.contains("tm-") ||
                    name.contains("star") || name.contains("epson")) {
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
     * Aggiorna la configurazione e reinizializza la stampante.
     */
    public void updateConfiguration(PrinterConfiguration newConfig) {
        this.config = newConfig;
        PrinterConfigurationDAO dao = new PreferencesPrinterConfigurationDAO();
        dao.save(newConfig);
        initializePrinter();
    }

    public PrinterConfiguration getConfiguration() {
        return config;
    }

    /**
     * Shutdown del servizio - chiamare alla chiusura dell'applicazione.
     */
    public void shutdown() {
        printExecutor.shutdown();
    }
}