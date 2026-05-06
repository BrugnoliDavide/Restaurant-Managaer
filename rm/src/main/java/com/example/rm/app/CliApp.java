package com.example.rm.app;

import com.example.rm.controller.FinancialService;
import com.example.rm.controller.FinancialUseCase;
import com.example.rm.controller.ManagerService;
import com.example.rm.controller.ManagerUseCase;
import com.example.rm.controller.MenuService;
import com.example.rm.controller.MenuUseCase;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.dao.ProductDAO;
import com.example.rm.exception.AuthenticationException;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.service.ConnectionManager;
import com.example.rm.service.SecurityService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Interfaccia a riga di comando (CLI) per Restaurant Manager.
 * <p>
 * Consente al <strong>manager</strong> di eseguire le operazioni principali
 * senza avviare l'interfaccia grafica JavaFX.
 */
public class CliApp {

    // =====================================================================
    //  Logger CLI — output pulito verso stdout
    // =====================================================================

    private static final Logger CLI = Logger.getLogger("cli.output");
    private static final String space0STR = "  {0}";
    private static final String spaceBAR0STR = "  ╠{0}╣";


    static {
        CLI.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord logEntry) {                  // S1219: no 'record'
                return logEntry.getMessage() + System.lineSeparator();
            }
        });
        handler.setLevel(Level.ALL);
        CLI.setLevel(Level.ALL);
        CLI.addHandler(handler);
    }

    private static final Logger APP_LOG = Logger.getLogger(CliApp.class.getName());

    // =====================================================================
    //  Costanti
    // =====================================================================

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String SEP =
            "════════════════════════════════════════════════════════════════";
    private static final String THIN =
            "────────────────────────────────────────────────────────────────";
    private static final String L35  = "─".repeat(35);
    private static final String L73  = "─".repeat(73);
    private static final String L98  = "─".repeat(98);
    private static final String BBAR = "══════════════════════════════════════════";

    private static final String CHOICE           = "  Scelta: ";
    private static final String BAD_CHOICE       = "  ⚠ Scelta non valida.";
    private static final String OP_CANCELLED     = "  Operazione annullata.";
    private static final String BAD_ID           = "  ✖ ID non valido.";
    private static final String BAD_VALUE        = "  ✖ Valore non valido.";
    private static final String NO_ORDERS        = "  Nessun ordine trovato.";
    private static final String PRODUCT_NOT_FOUND = "  ✖ Prodotto con ID {0} non trovato.";

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int MIN_PASSWORD_LEN   = 6;

    // =====================================================================
    //  Servizi
    // =====================================================================

    private final ManagerUseCase   managerUseCase;
    private final MenuUseCase      menuUseCase;
    private final FinancialUseCase financialUseCase;
    private final ProductDAO       productDAO;

    // =====================================================================
    //  Stato sessione
    // =====================================================================

    private String loggedUsername;
    private String loggedRole;

    // =====================================================================
    //  Costruttori
    // =====================================================================

    public CliApp() {
        this.managerUseCase   = new ManagerService();
        this.menuUseCase      = new MenuService();
        this.financialUseCase = new FinancialService();
        this.productDAO       = new DatabaseProductDAO();
    }

    CliApp(ManagerUseCase mu, MenuUseCase meu, FinancialUseCase fu, ProductDAO pd) {
        this.managerUseCase   = mu;
        this.menuUseCase      = meu;
        this.financialUseCase = fu;
        this.productDAO       = pd;
    }

    // =====================================================================
    //  Entry point
    // =====================================================================

    public static void main(String[] args) {
        new CliApp().run();
    }

    void run() {
        try (Scanner sc = new Scanner(System.in)) {
            printBanner();
            configureDatabase(sc);

            if (!login(sc)) {
                CLI.info("  ✖ Autenticazione fallita. Uscita.");
                return;
            }

            if (!"manager".equalsIgnoreCase(loggedRole)) {
                CLI.log(Level.INFO, "  ⚠ Questa CLI è riservata al ruolo manager. Ruolo corrente: {0}", loggedRole);
                return;
            }

            mainLoop(sc);
            CLI.info("\nArrivederci!");
        }
    }

    // =====================================================================
    //  Banner
    // =====================================================================

    private static void printBanner() {
        CLI.info(SEP);
        CLI.info("     RESTAURANT MANAGER — Interfaccia CLI");
        CLI.info(SEP);
        CLI.info("");
    }

    // =====================================================================
    //  Configurazione database
    // =====================================================================

    private static void configureDatabase(Scanner sc) {
        CLI.info("─── Configurazione Database ───\n");
        tryLoadSavedPreferences();

        if (safeTestConnection()) {
            CLI.info("  ✔ Connessione al database riuscita (configurazione salvata).\n");
            return;
        }

        CLI.info("Nessuna configurazione valida trovata. Inserire i parametri:\n");
        promptDatabaseUntilConnected(sc);
    }

    private static void tryLoadSavedPreferences() {
        try {
            ConnectionManager.loadFromPreferences();
        } catch (Exception e) {
            APP_LOG.log(Level.FINE, "Nessuna preferenza DB salvata: {0}", e.getMessage());
        }
    }

    /**
     * Ripete la richiesta dei parametri DB finché la connessione non riesce.
     * Nessun {@code break} né {@code continue}: il ciclo termina solo con
     * {@code return} in caso di successo.
     */
    private static void promptDatabaseUntilConnected(Scanner sc) {
        while (!attemptDatabaseConnection(sc)) {
            CLI.info("  ✖ Connessione fallita. Riprovare.\n");
        }
    }

    /**
     * Esegue un singolo tentativo di connessione.
     *
     * @return {@code true} se la connessione è riuscita
     */
    private static boolean attemptDatabaseConnection(Scanner sc) {
        String host   = promptWithDefault(sc, "  Host [localhost]: ", "localhost");
        String port   = promptWithDefault(sc, "  Porta [5432]: ", "5432");
        String dbName = prompt(sc, "  Nome database: ");
        String dbUser = prompt(sc, "  Username DB: ");
        String dbPass = prompt(sc, "  Password DB: ");

        if (dbName.isEmpty() || dbUser.isEmpty() || dbPass.isEmpty()) {
            CLI.info("  ✖ Nome database, username e password sono obbligatori.\n");
            return false;
        }

        ConnectionManager.configure(host, port, dbName, dbUser, dbPass);

        if (safeTestConnection()) {
            CLI.info("  ✔ Connessione al database riuscita.\n");
            return true;
        }

        String err = ConnectionManager.getLastConnectionError();
        if (err != null) {
            CLI.log(Level.INFO, "  ✖ Dettaglio errore: {0}", err);
        }
        return false;
    }

    private static boolean safeTestConnection() {
        try {
            return ConnectionManager.testConnection();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // =====================================================================
    //  Login
    // =====================================================================

    private boolean login(Scanner sc) {
        CLI.info("─── Login ───\n");
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String user = prompt(sc, "  Username: ");
            String pass = prompt(sc, "  Password: ");

            if (tryAuthenticate(user, pass, attempt)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryAuthenticate(String user, String pass, int attempt) {
        try {
            String role = SecurityService.authenticate(user, pass);
            loggedUsername = user;
            loggedRole = role;
            CLI.log(Level.INFO, "  ✔ Benvenuto, {0} [{1}]", new Object[]{user, role});
            return true;
        } catch (AuthenticationException e) {
            CLI.log(Level.INFO, "  ✖ {0} (tentativo {1}/{2})\n",
                    new Object[]{e.getUserMessage(), attempt, MAX_LOGIN_ATTEMPTS});
        } catch (Exception e) {
            CLI.log(Level.INFO, "  ✖ Errore imprevisto: {0} (tentativo {1}/{2})\n",
                    new Object[]{e.getMessage(), attempt, MAX_LOGIN_ATTEMPTS});
        }
        return false;
    }

    // =====================================================================
    //  Loop principale
    // =====================================================================

    private void mainLoop(Scanner sc) {
        boolean running = true;
        while (running) {
            printMainMenu();
            switch (sc.nextLine().trim()) {
                case "1" -> staffMenu(sc);
                case "2" -> menuMenu(sc);
                case "3" -> ordersMenu(sc);
                case "4" -> changePassword(sc);
                case "0" -> running = false;
                default  -> CLI.info(BAD_CHOICE);
            }
        }
    }

    private void printMainMenu() {
        CLI.info("");
        CLI.info(SEP);
        CLI.log(Level.INFO, "  MENU PRINCIPALE — {0}", loggedUsername);
        CLI.info(SEP);
        CLI.info("  1. Gestione Staff");
        CLI.info("  2. Gestione Menù");
        CLI.info("  3. Ordini e Report");
        CLI.info("  4. Cambia Password");
        CLI.info("  0. Esci");
        CLI.info(THIN);
        CLI.info(CHOICE);
    }

    // =====================================================================
    //  1. GESTIONE STAFF
    // =====================================================================

    private void staffMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            printSubMenu("GESTIONE STAFF",
                    "1. Elenco utenti", "2. Aggiungi utente", "3. Elimina utente");
            switch (sc.nextLine().trim()) {
                case "1" -> listUsers();
                case "2" -> addUser(sc);
                case "3" -> deleteUser(sc);
                case "0" -> back = true;
                default  -> CLI.info(BAD_CHOICE);
            }
        }
    }

    private void listUsers() {
        List<User> users = managerUseCase.loadAllUsers();
        CLI.info("");
        if (users.isEmpty()) {
            CLI.info("  Nessun utente registrato.");
            return;
        }
        logf("  %-20s %-15s", "USERNAME", "RUOLO");
        CLI.log(Level.INFO, space0STR, L35);
        for (User u : users) {
            logf("  %-20s %-15s", u.getUsername(), u.getRole());
        }
        CLI.log(Level.INFO, "  Totale: {0} utenti.", users.size());
    }

    private void addUser(Scanner sc) {
        CLI.info("");
        String u = prompt(sc, "  Username nuovo utente: ");
        String p = prompt(sc, "  Password: ");
        CLI.info("  Ruoli disponibili: manager, cameriere, cucina, cassiere");
        String r = prompt(sc, "  Ruolo: ").toLowerCase();

        if (u.isEmpty() || p.isEmpty() || r.isEmpty()) {
            CLI.info("  ✖ Tutti i campi sono obbligatori.");
            return;
        }
        if (!List.of("manager", "cameriere", "cucina", "cassiere").contains(r)) {
            CLI.info("  ✖ Ruolo non valido.");
            return;
        }

        boolean ok = SecurityService.registerUser(u, p, r);
        if (ok) {
            CLI.log(Level.INFO, "  ✔ Utente «{0}» creato con ruolo «{1}».", new Object[]{u, r});
        } else {
            CLI.info("  ✖ Impossibile creare l''utente (username già esistente?).");
        }
    }

    private void deleteUser(Scanner sc) {
        listUsers();
        CLI.info("");
        String target = prompt(sc, "  Username da eliminare: ");

        if (target.isEmpty()) {
            CLI.info("  ✖ Username non fornito.");
            return;
        }
        if (target.equalsIgnoreCase(loggedUsername)) {
            CLI.info("  ✖ Non è possibile eliminare l''utente attualmente collegato.");
            return;
        }
        if (!confirm(sc, "  Conferma eliminazione di «" + target + "»? (s/n): ")) {
            CLI.info(OP_CANCELLED);
            return;
        }

        boolean ok = managerUseCase.deleteUser(target);
        if (ok) {
            CLI.log(Level.INFO, "  ✔ Utente «{0}» eliminato.", target);
        } else {
            CLI.info("  ✖ Utente non trovato o errore durante l''eliminazione.");
        }
    }

    // =====================================================================
    //  2. GESTIONE MENÙ
    // =====================================================================

    private void menuMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            printSubMenu("GESTIONE MENÙ",
                    "1. Elenco prodotti", "2. Aggiungi prodotto",
                    "3. Modifica prodotto", "4. Elimina prodotto");
            switch (sc.nextLine().trim()) {
                case "1" -> listProducts();
                case "2" -> addProduct(sc);
                case "3" -> editProduct(sc);
                case "4" -> deleteProduct(sc);
                case "0" -> back = true;
                default  -> CLI.info(BAD_CHOICE);
            }
        }
    }

    private void listProducts() {
        List<MenuProduct> products = menuUseCase.loadAllProducts();
        CLI.info("");
        if (products.isEmpty()) {
            CLI.info("  Nessun prodotto nel menù.");
            return;
        }

        logf("  %-5s %-25s %-15s %10s %10s %8s  %-20s",
                "ID", "NOME", "CATEGORIA", "PREZZO", "COSTO", "MARG.%", "ALLERGENI");
        CLI.log(Level.INFO, space0STR, L98);

        for (MenuProduct p : products) {
            logf("  %-5d %-25s %-15s %10.2f %10.2f %7d%%  %-20s",
                    p.getId(),
                    truncate(p.getNome(), 25),
                    truncate(p.getTipologia(), 15),
                    p.getPrezzoVendita(),
                    p.getCostoRealizzazione(),
                    p.getPercentualeMargine(),
                    truncate(p.getAllergeni(), 20));
        }
        CLI.log(Level.INFO, "  Totale: {0} prodotti.", products.size());
    }

    private void addProduct(Scanner sc) {
        CLI.info("\n  ─── Nuovo prodotto ───");

        String nome = prompt(sc, "  Nome: ");
        if (nome.isEmpty()) { CLI.info("  ✖ Nome obbligatorio."); return; }

        List<String> categorie = menuUseCase.loadCategories();
        if (!categorie.isEmpty()) {
            CLI.log(Level.INFO, "  Categorie esistenti: {0}", String.join(", ", categorie));
        }
        String tipologia = prompt(sc, "  Categoria (tipologia): ");
        if (tipologia.isEmpty()) { CLI.info("  ✖ Categoria obbligatoria."); return; }

        BigDecimal prezzo = readBigDecimal(sc, "  Prezzo di vendita (€): ");
        if (prezzo == null) return;

        BigDecimal costo = readBigDecimal(sc, "  Costo di realizzazione (€): ");
        if (costo == null) return;

        String allergeni = prompt(sc, "  Allergeni (vuoto se nessuno): ");

        MenuProduct product = new MenuProduct(nome, tipologia, prezzo, costo, allergeni);
        boolean ok = menuUseCase.addProduct(product);
        if (ok) {
            CLI.log(Level.INFO, "  ✔ Prodotto «{0}» aggiunto al menù.", nome);
        } else {
            CLI.info("  ✖ Errore durante l''inserimento.");
        }
    }

    private void editProduct(Scanner sc) {
        listProducts();
        CLI.info("");
        int id = readInt(sc, "  ID del prodotto da modificare: ");
        if (id <= 0) { CLI.info(BAD_ID); return; }

        MenuProduct existing = menuUseCase.getProductById(id);
        if (existing == null) {
            CLI.log(Level.INFO, PRODUCT_NOT_FOUND, id);
            return;
        }

        CLI.info("  (Premere Invio per mantenere il valore attuale)\n");

        applyIfNotEmpty(prompt(sc, "  Nome [" + existing.getNome() + "]: "),
                existing::setNome);
        applyIfNotEmpty(prompt(sc, "  Categoria [" + existing.getTipologia() + "]: "),
                existing::setTipologia);

        if (!editBigDecimalField(sc, existing.getPrezzoVendita(), "Prezzo vendita",
                existing::setPrezzoVendita)) {
            return;
        }
        if (!editBigDecimalField(sc, existing.getCostoRealizzazione(), "Costo realizzazione",
                existing::setCostoRealizzazione)) {
            return;
        }

        applyIfNotEmpty(prompt(sc, "  Allergeni [" + existing.getAllergeni() + "]: "),
                existing::setAllergeni);

        boolean ok = menuUseCase.updateProduct(existing);
        if (ok) {
            CLI.info("  ✔ Prodotto aggiornato.");
        } else {
            CLI.info("  ✖ Errore durante l''aggiornamento.");
        }
    }

    private static boolean editBigDecimalField(Scanner sc, BigDecimal current,
                                               String label, Consumer<BigDecimal> setter) {
        String raw = prompt(sc, "  " + label + " [" + current + "]: ");
        if (raw.isEmpty()) {
            return true;
        }
        BigDecimal val = parseBigDecimalOrNull(raw);
        if (val == null) {
            CLI.info(BAD_VALUE);
            return false;
        }
        setter.accept(val);
        return true;
    }

    private void deleteProduct(Scanner sc) {
        listProducts();
        CLI.info("");
        int id = readInt(sc, "  ID del prodotto da eliminare: ");
        if (id <= 0) { CLI.info(BAD_ID); return; }

        MenuProduct existing = menuUseCase.getProductById(id);
        if (existing == null) {
            CLI.log(Level.INFO, PRODUCT_NOT_FOUND, id);
            return;
        }
        if (!confirm(sc, "  Conferma eliminazione di «" + existing.getNome() + "»? (s/n): ")) {
            CLI.info(OP_CANCELLED);
            return;
        }

        boolean ok = productDAO.delete((long) id);
        if (ok) {
            CLI.info("  ✔ Prodotto eliminato.");
        } else {
            CLI.info("  ✖ Errore durante l''eliminazione.");
        }
    }

    // =====================================================================
    //  3. ORDINI E REPORT
    // =====================================================================

    private void ordersMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            printSubMenu("ORDINI E REPORT",
                    "1. Tutti gli ordini (con totale)", "2. Dettaglio di un ordine",
                    "3. Ordini da pagare", "4. Ordini attivi in cucina");
            switch (sc.nextLine().trim()) {
                case "1" -> listAllOrders();
                case "2" -> orderDetail(sc);
                case "3" -> listOrdersToPay();
                case "4" -> listKitchenActive();
                case "0" -> back = true;
                default  -> CLI.info(BAD_CHOICE);
            }
        }
    }

    private void listAllOrders() {
        printOrderTable(financialUseCase.loadAllOrdersWithTotal(), "TUTTI GLI ORDINI");
    }

    private static void listOrdersToPay() {
        printOrderTable(com.example.rm.service.OrderService.getToPay(), "ORDINI DA PAGARE");
    }

    private static void listKitchenActive() {
        printOrderTable(com.example.rm.service.OrderService.getKitchenActive(),
                "ORDINI ATTIVI IN CUCINA");
    }

    private static void printOrderTable(List<Order> orders, String title) {
        CLI.log(Level.INFO, "\n  {0}", title);
        CLI.log(Level.INFO, space0STR, "─".repeat(title.length()));

        if (orders.isEmpty()) {
            CLI.info(NO_ORDERS);
            return;
        }

        logf("  %-6s %-18s %-8s %-15s %-12s %10s",
                "ID", "DATA/ORA", "TAVOLO", "CAMERIERE", "STATO", "TOTALE €");
        CLI.log(Level.INFO, space0STR, L73);

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Order o : orders) {
            logf("  %-6d %-18s %-8d %-15s %-12s %10.2f",
                    o.getId(),
                    o.getDataOra().format(DTF),
                    o.getTavolo(),
                    truncate(o.getUsername(), 15),
                    o.getStatus(),
                    o.getTotale());
            grandTotal = grandTotal.add(o.getTotale());
        }
        CLI.log(Level.INFO, space0STR, L73);
        logf("  %d ordini — Totale complessivo: €%.2f", orders.size(), grandTotal);
    }

    private static void orderDetail(Scanner sc) {
        CLI.info("");
        int id = readInt(sc, "  ID ordine: ");
        if (id <= 0) { CLI.info(BAD_ID); return; }

        Order order = com.example.rm.service.OrderService.findById(id);
        if (order == null) {
            CLI.log(Level.INFO, "  ✖ Ordine #{0} non trovato.", id);
            return;
        }

        printOrderHeader(order);
        printOrderItems(order.getId());
        CLI.log(Level.INFO, "  ╚{0}╝", BBAR);
    }

    private static void printOrderHeader(Order order) {
        CLI.info("");
        CLI.log(Level.INFO, "  ╔{0}╗", BBAR);
        logf("  ║  ORDINE #%-33d║", order.getId());
        CLI.log(Level.INFO, spaceBAR0STR, BBAR);
        logf("  ║  Data:      %-30s║", order.getDataOra().format(DTF));
        logf("  ║  Tavolo:    %-30d║", order.getTavolo());
        logf("  ║  Cameriere: %-30s║", order.getUsername());
        logf("  ║  Stato:     %-30s║", order.getStatus());
        logf("  ║  Totale:    €%-29.2f║", order.getTotale());
        if (order.hasNote()) {
            logf("  ║  Note:      %-30s║", truncate(order.getNote(), 30));
        }
        CLI.log(Level.INFO, spaceBAR0STR, BBAR);
        CLI.info("  ║  ARTICOLI                                ║");
        CLI.log(Level.INFO, spaceBAR0STR, BBAR);
    }

    private static void printOrderItems(int orderId) {
        List<OrderItem> items =
                com.example.rm.service.OrderService.getItemsDetailed(orderId);

        if (items.isEmpty()) {
            CLI.info("  ║  (nessun articolo)                       ║");
            return;
        }
        for (OrderItem item : items) {
            BigDecimal lineTotal = item.getPrezzoSnapshot()
                    .multiply(BigDecimal.valueOf(item.getQuantita()));
            logf("  ║  • %-39s║",
                    truncate(item.getQuantita() + "x "
                            + item.getDisplayName() + " — €" + lineTotal, 39));
        }
    }

    // =====================================================================
    //  4. CAMBIO PASSWORD
    // =====================================================================

    private void changePassword(Scanner sc) {
        CLI.info("\n  ─── Cambio Password ───");
        String current = prompt(sc, "  Password attuale: ");
        String newPass = prompt(sc, "  Nuova password (min 6 caratteri): ");
        String conf    = prompt(sc, "  Conferma nuova password: ");

        if (newPass.length() < MIN_PASSWORD_LEN) {
            CLI.info("  ✖ La password deve essere di almeno 6 caratteri.");
            return;
        }
        if (!newPass.equals(conf)) {
            CLI.info("  ✖ Le password non corrispondono.");
            return;
        }

        boolean ok = SecurityService.changePassword(loggedUsername, current, newPass);
        if (ok) {
            CLI.info("  ✔ Password aggiornata con successo.");
        } else {
            CLI.info("  ✖ Password attuale errata o errore durante l''aggiornamento.");
        }
    }

    // =====================================================================
    //  Utilità — I/O
    // =====================================================================

    private static String prompt(Scanner sc, String message) {
        CLI.info(message);
        return sc.nextLine().trim();
    }

    private static String promptWithDefault(Scanner sc, String message, String defaultVal) {
        String input = prompt(sc, message);
        return input.isEmpty() ? defaultVal : input;
    }

    private static boolean confirm(Scanner sc, String message) {
        return "s".equalsIgnoreCase(prompt(sc, message));
    }

    private static int readInt(Scanner sc, String message) {
        String raw = prompt(sc, message);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static BigDecimal readBigDecimal(Scanner sc, String message) {
        String raw = prompt(sc, message).replace(",", ".");
        BigDecimal val = parseBigDecimalOrNull(raw);
        if (val == null) {
            CLI.info("  ✖ Valore numerico non valido.");
            return null;
        }
        if (val.compareTo(BigDecimal.ZERO) < 0) {
            CLI.info("  ✖ Il valore non può essere negativo.");
            return null;
        }
        return val;
    }

    private static BigDecimal parseBigDecimalOrNull(String raw) {
        try {
            return new BigDecimal(raw.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =====================================================================
    //  Utilità — formattazione
    // =====================================================================

    /**
     * Scrive un messaggio formattato (stile {@code printf}) sul logger CLI.
     * La costruzione della stringa avviene solo se il livello INFO è attivo,
     * in conformità a SonarCloud S2629.
     */
    private static void logf(String format, Object... args) {
        if (CLI.isLoggable(Level.INFO)) {
            CLI.info(String.format(format, args));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void applyIfNotEmpty(String value, Consumer<String> setter) {
        if (!value.isEmpty()) {
            setter.accept(value);
        }
    }

    private static void printSubMenu(String title, String... options) {
        CLI.info("");
        CLI.info(THIN);
        CLI.log(Level.INFO, space0STR, title);
        CLI.info(THIN);
        for (String opt : options) {
            CLI.log(Level.INFO, space0STR, opt);
        }
        CLI.info("  0. Indietro");
        CLI.info(CHOICE);
    }
}