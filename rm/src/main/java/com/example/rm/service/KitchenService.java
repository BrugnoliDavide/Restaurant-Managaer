package com.example.rm.service;

import com.example.rm.dao.DatabaseKitchenPreferencesDAO;
import com.example.rm.dao.KitchenPreferencesDAO;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.printer.PrinterService;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestisce le preferenze di cucina e il filtraggio degli ordini attivi
 * in base alle categorie selezionate dall'utente cucina.

 * ESTESO CON: Integrazione stampa comande automatica su stampante termica ESC/POS.
 */
public final class KitchenService {

    private static final Logger LOGGER = Logger.getLogger(KitchenService.class.getName());

    private static final KitchenPreferencesDAO preferencesDAO =
            new DatabaseKitchenPreferencesDAO();

    private KitchenService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Recupera le preferenze cucina per un utente.
     */
    public static KitchenPreferences getPreferences(String username) {
        return preferencesDAO.loadByUsername(username);
    }

    /**
     * Restituisce gli ordini attivi filtrati secondo le preferenze dell'utente cucina.
     *
     * <p>Se l'utente ha attivato {@code includeOtherCategories}, vengono restituiti
     * tutti gli ordini attivi. Altrimenti vengono inclusi solo gli ordini i cui
     * articoli appartengono interamente alle categorie selezionate.</p>
     *
     * <p><strong>ESTENSIONE STAMPA:</strong> Quando un nuovo ordine viene visualizzato
     * per la prima volta in cucina, viene automaticamente inviato alla stampante
     * termica se la stampa è abilitata nelle preferenze dell'utente.</p>
     *
     * @param username username dell'utente cucina
     * @return lista di ordini filtrati
     */
    public static List<Order> getActiveOrdersFiltered(String username) {
        KitchenPreferences prefs = getPreferences(username);
        List<Order> allOrders = OrderService.getKitchenActive();

        // Se nessuna categoria selezionata mostra tutto
        if (prefs.isIncludeOtherCategories() || prefs.getSelectedCategories().isEmpty()) {
            // STAMPA AUTOMATICA: Stampa tutti gli ordini nuovi
            printNewOrdersIfEnabled(allOrders, username, prefs);
            return allOrders;
        }

        List<Order> filtered = new ArrayList<>();
        for (Order order : allOrders) {
            List<OrderItem> items = OrderService.getItemsDetailed(order.getId());
            Set<String> orderCategories = categoriesOf(items);

            // delega a KitchenPreferences che è il posto corretto per questa logica
            if (prefs.shouldDisplayOrder(orderCategories)) {
                filtered.add(order);
            }
        }

        // STAMPA AUTOMATICA: Stampa solo gli ordini filtrati che sono nuovi
        printNewOrdersIfEnabled(filtered, username, prefs);

        return filtered;
    }

    /**
     * Stampa automaticamente i nuovi ordini su stampante termica ESC/POS.
     *
     * <p>Questo metodo controlla quali ordini sono stati visualizzati per la prima volta
     * e, se la stampa è abilitata nelle preferenze dell'utente, li invia alla stampante
     * in modo asincrono (senza bloccare l'interfaccia utente).</p>
     *
     * <p><strong>Gestione errori:</strong> Eventuali errori di stampa vengono loggati
     * ma NON interrompono il flusso principale. La cucina può sempre visualizzare
     * gli ordini sullo schermo anche se la stampante è offline.</p>
     *
     * <p><strong>Performance:</strong> La stampa avviene su un thread separato per
     * non rallentare l'aggiornamento dell'interfaccia utente.</p>
     *
     * @param orders lista degli ordini da potenzialmente stampare
     * @param username username dell'utente cucina (per recuperare le preferenze)
     * @param prefs preferenze dell'utente cucina (null = carica automaticamente)
     */
    private static void printNewOrdersIfEnabled(List<Order> orders, String username,
                                                KitchenPreferences prefs) {
        // Controllo preliminare: se nessun ordine, esci immediatamente
        if (orders == null || orders.isEmpty()) {
            return;
        }

        // Se le preferenze non sono state passate, caricale
        if (prefs == null) {
            prefs = getPreferences(username);
        }

        // Verifica se la stampa è abilitata nelle preferenze
        // NOTA: Questo richiede l'aggiunta del campo 'printOrdersAutomatically'
        // alla classe KitchenPreferences (vedi sotto per dettagli)
        if (prefs == null || !prefs.isPrintOrdersAutomatically()) {
            LOGGER.log(Level.INFO,"Stampa comande disabilitata per utente: {0}}", username);
            return;
        }

        // Ottieni il servizio di stampa (singleton)
        PrinterService printerService = PrinterService.getInstance();

        // Verifica che il servizio stampante sia disponibile e configurato
        if (!printerService.getConfiguration().isEnabled()) {
            LOGGER.fine("Stampante non configurata o disabilitata a livello globale");
            return;
        }

        // Traccia quali ordini sono stati già stampati (cache in memoria)
        // In produzione, questo potrebbe essere persistito in DB
        Set<Integer> printedOrderIds = getPrintedOrderIdsCache();

        // Itera sugli ordini e stampa solo quelli nuovi
        for (Order order : orders) {
            // Verifica se questo ordine è già stato stampato
            if (printedOrderIds.contains(order.getId())) {
                // Ordine già stampato in precedenza, salta
                continue;
            }

            try {
                // Recupera gli articoli dell'ordine con tutti i dettagli
                List<OrderItem> items = OrderService.getItemsDetailed(order.getId());

                // Invia alla stampante in modo ASINCRONO (non blocca l'UI)
                printerService.printOrderAsync(order, items);

                // Segna l'ordine come stampato per evitare duplicati
                printedOrderIds.add(order.getId());

                LOGGER.log(Level.INFO,
                        "Comanda inviata alla stampante - Ordine #{0}, Tavolo {1}",
                        new Object[]{order.getId(), order.getTavolo()});

            } catch (Exception e) {
                // CRITICO: Non interrompere il flusso anche se la stampa fallisce
                // La cucina DEVE comunque vedere l'ordine sullo schermo
                LOGGER.log(Level.WARNING,
                        "Errore durante la stampa di ordine #{0} - ordine visibile sullo schermo", order.getId());
            }
        }
    }

    /**
     * Stampa manualmente un ordine specifico su richiesta dell'utente.
     *
     * <p>Questo metodo può essere chiamato dal controller quando l'utente
     * clicca su un pulsante "Ristampa" nella UI.</p>
     *
     * @param orderId ID dell'ordine da stampare
     * @return true se la stampa è stata avviata con successo, false altrimenti
     */
    public static boolean printOrderManually(int orderId) {
        try {
            PrinterService printerService = PrinterService.getInstance();

            if (!printerService.getConfiguration().isEnabled()) {
                LOGGER.log(Level.WARNING,"Tentativo di stampa manuale ma stampante disabilitata");
                return false;
            }

            Order order = OrderService.findById(orderId);
            if (order == null) {
                LOGGER.log(Level.WARNING,"Tentativo di stampare ordine inesistente: {0}", orderId);
                return false;
            }

            List<OrderItem> items = OrderService.getItemsDetailed(orderId);

            // Stampa SINCRONA per dare feedback immediato all'utente
            printerService.printOrderSync(order, items);

            LOGGER.log(Level.INFO,"Ristampa manuale completata per ordine #{0}", orderId);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Errore durante la ristampa manuale ordine #{0}", orderId);
            return false;
        }
    }

    /**
     * Cache in-memory degli ordini già stampati.
     *
     * <p><strong>IMPLEMENTAZIONE SEMPLIFICATA:</strong> Usa un Set statico che
     * persiste solo durante l'esecuzione dell'applicazione. In produzione,
     * considerare di persistere questo stato in database con un campo
     * 'printed_at' nella tabella orders.</p>
     *
     * <p><strong>GESTIONE MEMORIA:</strong> La cache viene pulita automaticamente
     * quando gli ordini vengono completati/rimossi dalla vista cucina.</p>
     *
     * @return Set contenente gli ID degli ordini già stampati
     */
    private static Set<Integer> getPrintedOrderIdsCache() {
        // Implementazione semplificata: Set statico
        // In produzione, usare un DB o un sistema di cache distribuito (Redis)
        return PrintedOrdersCache.getInstance().getPrintedIds();
    }

    /**
     * Pulisce la cache di stampa per un ordine specifico.
     * Da chiamare quando un ordine viene completato o rimosso.
     *
     * @param orderId ID dell'ordine da rimuovere dalla cache
     */
    public static void clearPrintedOrderCache(int orderId) {
        getPrintedOrderIdsCache().remove(orderId);
        LOGGER.log(Level.INFO,"Cache di stampa pulita per ordine #{0}",orderId);
    }

    /**
     * Estrae l'insieme delle categorie (tipologie) presenti negli articoli.
     */
    private static Set<String> categoriesOf(List<OrderItem> items) {
        Set<String> categories = new HashSet<>();
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getProduct() != null && item.getProduct().getTipologia() != null) {
                    categories.add(item.getProduct().getTipologia());
                }
            }
        }
        return categories;
    }

    /**
     * Classe interna singleton per gestire la cache degli ordini stampati.
     * Thread-safe.
     */
    private static class PrintedOrdersCache {
        private static final PrintedOrdersCache INSTANCE = new PrintedOrdersCache();
        private final Set<Integer> printedIds = Collections.synchronizedSet(new HashSet<>());

        private PrintedOrdersCache() {}

        public static PrintedOrdersCache getInstance() {
            return INSTANCE;
        }

        public Set<Integer> getPrintedIds() {
            return printedIds;
        }
    }
}