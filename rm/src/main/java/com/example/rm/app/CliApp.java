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
 * <p>
 * L'output verso il terminale avviene tramite un {@link Logger} dedicato
 * con un {@link Formatter} che produce testo pulito (senza timestamp né
 * livello di log), in conformità alla regola SonarCloud java:S106.
 */
public class CliApp {

    // =====================================================================
    //  Logger CLI — output pulito verso stdout
    // =====================================================================

    private static final Logger CLI = Logger.getLogger("cli.output");

    static {
        CLI.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });
        handler.setLevel(Level.ALL);
        CLI.setLevel(Level.ALL);
        CLI.addHandler(handler);
    }

    /** Logger applicativo (diagnostica, non output utente). */
    private static final Logger APP_LOG = Logger.getLogger(CliApp.class.getName());

    // =====================================================================
    //  Costanti di formattazione
    // =====================================================================

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String SEPARATOR =
            "════════════════════════════════════════════════════════════════";
    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────────────";
    private static final String LINE_35  = "─".repeat(35);
    private static final String LINE_73  = "─".repeat(73);
    private static final String LINE_98  = "─".repeat(98);
    private static final String BOX_BAR  = "══════════════════════════════════════════";

    private static final String OK_PREFIX    = "  ✔ ";
    private static final String ERR_PREFIX   = "  ✖ ";
    private static final String WARN_PREFIX  = "  ⚠ ";

    private static final String CHOICE             = "  Scelta: ";
    private static final String MSG_INVALID_CHOICE = WARN_PREFIX + "Scelta non valida.";
    private static final String MSG_OP_CANCELLED   = "  Operazione annullata.";
    private static final String MSG_INVALID_ID     = ERR_PREFIX + "ID non valido.";
    private static final String MSG_INVALID_VALUE  = ERR_PREFIX + "Valore non valido.";

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    // =====================================================================
    //  Servizi (riuso del layer esistente)
    // =====================================================================

    private final ManagerUseCase   managerUseCase;
    private final MenuUseCase      menuUseCase;
    private final FinancialUseCase financialUseCase;
    private final ProductDAO       productDAO;

    // =====================================================================
    //  Stato della sessione
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

    /** Costruttore per test e dependency injection. */
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
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");

        new CliApp().run();
    }

    void run() {
        try (Scanner sc = new Scanner(System.in)) {
            printBanner();
            configureDatabase(sc);

            if (!login(sc)) {
                CLI.info(ERR_PREFIX + "Autenticazione fallita. Uscita.");
                return;
            }

            if (!"manager".equalsIgnoreCase(loggedRole)) {
                CLI.info(WARN_PREFIX + "Questa CLI è riservata al ruolo manager.");
                CLI.info("  Il tuo ruolo corrente è: " + loggedRole);
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
        CLI.info(SEPARATOR);
        CLI.info("     RESTAURANT MANAGER — Interfaccia CLI");
        CLI.info(SEPARATOR);
        CLI.info("");
    }

    // =====================================================================
    //  Configurazione database
    // =====================================================================

    private static void configureDatabase(Scanner sc) {
        CLI.info("─── Configurazione Database ───\n");
        tryLoadSavedPreferences();

        if (safeTestConnection()) {
            CLI.info(OK_PREFIX + "Connessione al database riuscita (configurazione salvata).\n");
            return;
        }

        CLI.info("Nessuna configurazione valida trovata. Inserire i parametri:\n");
        promptDatabaseParams(sc);
    }

    private static void tryLoadSavedPreferences() {
        try {
            ConnectionManager.loadFromPreferences();
        } catch (Exception e) {
            APP_LOG.log(Level.FINE, "Nessuna preferenza DB salvata: {0}", e.getMessage());
        }
    }

    private static void promptDatabaseParams(Scanner sc) {
        boolean connected = false;
        while (!connected) {
            String host   = promptWithDefault(sc, "  Host [localhost]: ", "localhost");
            String port   = promptWithDefault(sc, "  Porta [5432]: ", "5432");
            String dbName = prompt(sc, "  Nome database: ");
            String dbUser = prompt(sc, "  Username DB: ");
            String dbPass = prompt(sc, "  Password DB: ");

            if (dbName.isEmpty() || dbUser.isEmpty() || dbPass.isEmpty()) {
                CLI.info(ERR_PREFIX + "Nome database, username e password sono obbligatori.\n");
                continue;
            }

            ConnectionManager.configure(host, port, dbName, dbUser, dbPass);

            if (safeTestConnection()) {
                CLI.info(OK_PREFIX + "Connessione al database riuscita.\n");
                connected = true;
            } else {
                printConnectionError();
                if (!confirm(sc, "  Riprovare? (s/n): ")) {
                    CLI.info("  Il programma prosegue, ma le operazioni su DB falliranno.\n");
                    break;
                }
                CLI.info("");
            }
        }
    }

    private static void printConnectionError() {
        String err = ConnectionManager.getLastConnectionError();
        CLI.info(ERR_PREFIX + "Connessione fallita: "
                + (err != null ? err : "errore sconosciuto"));
    }

    /**
     * Invoca {@link ConnectionManager#testConnection()} intercettando
     * l'eventuale {@code IllegalStateException} dovuta al difetto noto
     * in {@code ConnectionManager.isConfigured()}.
     */
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
            CLI.info(OK_PREFIX + "Benvenuto, " + user + " [" + role + "]");
            return true;
        } catch (AuthenticationException e) {
            CLI.info(ERR_PREFIX + e.getUserMessage()
                    + " (tentativo " + attempt + "/" + MAX_LOGIN_ATTEMPTS + ")\n");
        } catch (Exception e) {
            CLI.info(ERR_PREFIX + "Errore imprevisto: " + e.getMessage()
                    + " (tentativo " + attempt + "/" + MAX_LOGIN_ATTEMPTS + ")\n");
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
                default  -> CLI.info(MSG_INVALID_CHOICE);
            }
        }
    }

    private void printMainMenu() {
        CLI.info("");
        CLI.info(SEPARATOR);
        CLI.info("  MENU PRINCIPALE — " + loggedUsername);
        CLI.info(SEPARATOR);
        CLI.info("  1. Gestione Staff");
        CLI.info("  2. Gestione Menù");
        CLI.info("  3. Ordini e Report");
        CLI.info("  4. Cambia Password");
        CLI.info("  0. Esci");
        CLI.info(THIN_SEP);
        CLI.info(CHOICE);
    }

    // =====================================================================
    //  1. GESTIONE STAFF
    // =====================================================================

    private void staffMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            printSubMenu("GESTIONE STAFF",
                    "1. Elenco utenti",
                    "2. Aggiungi utente",
                    "3. Elimina utente");
            switch (sc.nextLine().trim()) {
                case "1" -> listUsers();
                case "2" -> addUser(sc);
                case "3" -> deleteUser(sc);
                case "0" -> back = true;
                default  -> CLI.info(MSG_INVALID_CHOICE);
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
        CLI.info(String.format("  %-20s %-15s", "USERNAME", "RUOLO"));
        CLI.info("  " + LINE_35);
        for (User u : users) {
            CLI.info(String.format("  %-20s %-15s", u.getUsername(), u.getRole()));
        }
        CLI.info("  Totale: " + users.size() + " utenti.");
    }

    private void addUser(Scanner sc) {
        CLI.info("");
        String u = prompt(sc, "  Username nuovo utente: ");
        String p = prompt(sc, "  Password: ");
        CLI.info("  Ruoli disponibili: manager, cameriere, cucina, cassiere");
        String r = prompt(sc, "  Ruolo: ").toLowerCase();

        if (u.isEmpty() || p.isEmpty() || r.isEmpty()) {
            CLI.info(ERR_PREFIX + "Tutti i campi sono obbligatori.");
            return;
        }
        if (!List.of("manager", "cameriere", "cucina", "cassiere").contains(r)) {
            CLI.info(ERR_PREFIX + "Ruolo non valido.");
            return;
        }

        boolean ok = SecurityService.registerUser(u, p, r);
        CLI.info(ok
                ? OK_PREFIX + "Utente «" + u + "» creato con ruolo «" + r + "»."
                : ERR_PREFIX + "Impossibile creare l'utente (username già esistente?).");
    }

    private void deleteUser(Scanner sc) {
        listUsers();
        CLI.info("");
        String target = prompt(sc, "  Username da eliminare: ");

        if (target.isEmpty()) {
            CLI.info(ERR_PREFIX + "Username non fornito.");
            return;
        }
        if (target.equalsIgnoreCase(loggedUsername)) {
            CLI.info(ERR_PREFIX + "Non è possibile eliminare l'utente attualmente collegato.");
            return;
        }
        if (!confirm(sc, "  Conferma eliminazione di «" + target + "»? (s/n): ")) {
            CLI.info(MSG_OP_CANCELLED);
            return;
        }

        boolean ok = managerUseCase.deleteUser(target);
        CLI.info(ok
                ? OK_PREFIX + "Utente «" + target + "» eliminato."
                : ERR_PREFIX + "Utente non trovato o errore durante l'eliminazione.");
    }

    // =====================================================================
    //  2. GESTIONE MENÙ
    // =====================================================================

    private void menuMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            printSubMenu("GESTIONE MENÙ",
                    "1. Elenco prodotti",
                    "2. Aggiungi prodotto",
                    "3. Modifica prodotto",
                    "4. Elimina prodotto");
            switch (sc.nextLine().trim()) {
                case "1" -> listProducts();
                case "2" -> addProduct(sc);
                case "3" -> editProduct(sc);
                case "4" -> deleteProduct(sc);
                case "0" -> back = true;
                default  -> CLI.info(MSG_INVALID_CHOICE);
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

        CLI.info(String.format("  %-5s %-25s %-15s %10s %10s %8s  %-20s",
                "ID", "NOME", "CATEGORIA", "PREZZO", "COSTO", "MARG.%", "ALLERGENI"));
        CLI.info("  " + LINE_98);

        for (MenuProduct p : products) {
            CLI.info(String.format("  %-5d %-25s %-15s %10.2f %10.2f %7d%%  %-20s",
                    p.getId(),
                    truncate(p.getNome(), 25),
                    truncate(p.getTipologia(), 15),
                    p.getPrezzoVendita(),
                    p.getCostoRealizzazione(),
                    p.getPercentualeMargine(),
                    truncate(p.getAllergeni(), 20)));
        }
        CLI.info("  Totale: " + products.size() + " prodotti.");
    }

    private void addProduct(Scanner sc) {
        CLI.info("\n  ─── Nuovo prodotto ───");

        String nome = prompt(sc, "  Nome: ");
        if (nome.isEmpty()) { CLI.info(ERR_PREFIX + "Nome obbligatorio."); return; }

        List<String> categorie = menuUseCase.loadCategories();
        if (!categorie.isEmpty()) {
            CLI.info("  Categorie esistenti: " + String.join(", ", categorie));
        }
        String tipologia = prompt(sc, "  Categoria (tipologia): ");
        if (tipologia.isEmpty()) { CLI.info(ERR_PREFIX + "Categoria obbligatoria."); return; }

        BigDecimal prezzo = readBigDecimal(sc, "  Prezzo di vendita (€): ");
        if (prezzo == null) return;

        BigDecimal costo = readBigDecimal(sc, "  Costo di realizzazione (€): ");
        if (costo == null) return;

        String allergeni = prompt(sc, "  Allergeni (vuoto se nessuno): ");

        MenuProduct product = new MenuProduct(nome, tipologia, prezzo, costo, allergeni);
        boolean ok = menuUseCase.addProduct(product);
        CLI.info(ok
                ? OK_PREFIX + "Prodotto «" + nome + "» aggiunto al menù."
                : ERR_PREFIX + "Errore durante l'inserimento.");
    }

    private void editProduct(Scanner sc) {
        listProducts();
        CLI.info("");
        int id = readInt(sc, "  ID del prodotto da modificare: ");
        if (id <= 0) { CLI.info(MSG_INVALID_ID); return; }

        MenuProduct existing = menuUseCase.getProductById(id);
        if (existing == null) {
            CLI.info(ERR_PREFIX + "Prodotto con ID " + id + " non trovato.");
            return;
        }

        CLI.info("  (Premere Invio per mantenere il valore attuale)\n");

        applyIfNotEmpty(prompt(sc, "  Nome [" + existing.getNome() + "]: "),
                existing::setNome);
        applyIfNotEmpty(prompt(sc, "  Categoria [" + existing.getTipologia() + "]: "),
                existing::setTipologia);

        if (!editBigDecimalField(sc, "  Prezzo vendita [" + existing.getPrezzoVendita() + "]: ",
                existing::setPrezzoVendita)) {
            return;
        }
        if (!editBigDecimalField(sc, "  Costo realizzazione [" + existing.getCostoRealizzazione() + "]: ",
                existing::setCostoRealizzazione)) {
            return;
        }

        applyIfNotEmpty(prompt(sc, "  Allergeni [" + existing.getAllergeni() + "]: "),
                existing::setAllergeni);

        boolean ok = menuUseCase.updateProduct(existing);
        CLI.info(ok ? OK_PREFIX + "Prodotto aggiornato."
                : ERR_PREFIX + "Errore durante l'aggiornamento.");
    }

    /**
     * Legge un campo BigDecimal opzionale; se l'utente preme Invio,
     * il campo non viene modificato. Restituisce {@code false} se
     * il valore inserito non è valido.
     */
    private static boolean editBigDecimalField(Scanner sc, String message,
                                               java.util.function.Consumer<BigDecimal> setter) {
        String raw = prompt(sc, message);
        if (raw.isEmpty()) {
            return true; // campo non modificato
        }
        BigDecimal val = parseBigDecimalOrNull(raw);
        if (val == null) {
            CLI.info(MSG_INVALID_VALUE);
            return false;
        }
        setter.accept(val);
        return true;
    }

    private void deleteProduct(Scanner sc) {
        listProducts();
        CLI.info("");
        int id = readInt(sc, "  ID del prodotto da eliminare: ");
        if (id <= 0) { CLI.info(MSG_INVALID_ID); return; }

        MenuProduct existing = menuUseCase.getProductById(id);
        if (existing == null) {
            CLI.info(ERR_PREFIX + "Prodotto con ID " + id + " non trovato.");
            return;
        }
        if (!confirm(sc, "  Conferma eliminazione di «" + existing.getNome() + "»? (s/n): ")) {
            CLI.info(MSG_OP_CANCELLED);
            return;
        }

        boolean ok = productDAO.delete((long) id);
        CLI.info(ok ? OK_PREFIX + "Prodotto eliminato."
                : ERR_PREFIX + "Errore durante l'eliminazione.");
    }

    // =====================================================================
    //  3. ORDINI E REPORT
    // =====================================================================

    private void ordersMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            printSubMenu("ORDINI E REPORT",
                    "1. Tutti gli ordini (con totale)",
                    "2. Dettaglio di un ordine",
                    "3. Ordini da pagare",
                    "4. Ordini attivi in cucina");
            switch (sc.nextLine().trim()) {
                case "1" -> listAllOrders();
                case "2" -> orderDetail(sc);
                case "3" -> listOrdersToPay();
                case "4" -> listKitchenActive();
                case "0" -> back = true;
                default  -> CLI.info(MSG_INVALID_CHOICE);
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
        CLI.info("\n  " + title);
        CLI.info("  " + "─".repeat(title.length()));

        if (orders.isEmpty()) {
            CLI.info("  Nessun ordine trovato.");
            return;
        }

        CLI.info(String.format("  %-6s %-18s %-8s %-15s %-12s %10s",
                "ID", "DATA/ORA", "TAVOLO", "CAMERIERE", "STATO", "TOTALE €"));
        CLI.info("  " + LINE_73);

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Order o : orders) {
            CLI.info(String.format("  %-6d %-18s %-8d %-15s %-12s %10.2f",
                    o.getId(),
                    o.getDataOra().format(DTF),
                    o.getTavolo(),
                    truncate(o.getUsername(), 15),
                    o.getStatus(),
                    o.getTotale()));
            grandTotal = grandTotal.add(o.getTotale());
        }
        CLI.info("  " + LINE_73);
        CLI.info(String.format("  %d ordini — Totale complessivo: €%.2f",
                orders.size(), grandTotal));
    }

    private static void orderDetail(Scanner sc) {
        CLI.info("");
        int id = readInt(sc, "  ID ordine: ");
        if (id <= 0) { CLI.info(MSG_INVALID_ID); return; }

        Order order = com.example.rm.service.OrderService.findById(id);
        if (order == null) {
            CLI.info(ERR_PREFIX + "Ordine #" + id + " non trovato.");
            return;
        }

        printOrderDetailBox(order);
    }

    private static void printOrderDetailBox(Order order) {
        CLI.info("");
        CLI.info("  ╔" + BOX_BAR + "╗");
        CLI.info(String.format("  ║  ORDINE #%-33d║", order.getId()));
        CLI.info("  ╠" + BOX_BAR + "╣");
        CLI.info(String.format("  ║  Data:      %-30s║", order.getDataOra().format(DTF)));
        CLI.info(String.format("  ║  Tavolo:    %-30d║", order.getTavolo()));
        CLI.info(String.format("  ║  Cameriere: %-30s║", order.getUsername()));
        CLI.info(String.format("  ║  Stato:     %-30s║", order.getStatus()));
        CLI.info(String.format("  ║  Totale:    €%-29.2f║", order.getTotale()));
        if (order.hasNote()) {
            CLI.info(String.format("  ║  Note:      %-30s║", truncate(order.getNote(), 30)));
        }
        CLI.info("  ╠" + BOX_BAR + "╣");
        CLI.info("  ║  ARTICOLI                                ║");
        CLI.info("  ╠" + BOX_BAR + "╣");

        List<OrderItem> items =
                com.example.rm.service.OrderService.getItemsDetailed(order.getId());

        if (items.isEmpty()) {
            CLI.info("  ║  (nessun articolo)                       ║");
        } else {
            for (OrderItem item : items) {
                String line = String.format("%dx %s — €%.2f",
                        item.getQuantita(),
                        item.getDisplayName(),
                        item.getPrezzoSnapshot()
                                .multiply(BigDecimal.valueOf(item.getQuantita())));
                CLI.info(String.format("  ║  • %-39s║", truncate(line, 39)));
            }
        }
        CLI.info("  ╚" + BOX_BAR + "╝");
    }

    // =====================================================================
    //  4. CAMBIO PASSWORD
    // =====================================================================

    private void changePassword(Scanner sc) {
        CLI.info("\n  ─── Cambio Password ───");
        String current = prompt(sc, "  Password attuale: ");
        String newPass = prompt(sc, "  Nuova password (min 6 caratteri): ");
        String conf    = prompt(sc, "  Conferma nuova password: ");

        if (newPass.length() < 6) {
            CLI.info(ERR_PREFIX + "La password deve essere di almeno 6 caratteri.");
            return;
        }
        if (!newPass.equals(conf)) {
            CLI.info(ERR_PREFIX + "Le password non corrispondono.");
            return;
        }

        boolean ok = SecurityService.changePassword(loggedUsername, current, newPass);
        CLI.info(ok ? OK_PREFIX + "Password aggiornata con successo."
                : ERR_PREFIX + "Password attuale errata o errore durante l'aggiornamento.");
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
            CLI.info(ERR_PREFIX + "Valore numerico non valido.");
            return null;
        }
        if (val.compareTo(BigDecimal.ZERO) < 0) {
            CLI.info(ERR_PREFIX + "Il valore non può essere negativo.");
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

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void applyIfNotEmpty(String value,
                                        java.util.function.Consumer<String> setter) {
        if (!value.isEmpty()) {
            setter.accept(value);
        }
    }

    private static void printSubMenu(String title, String... options) {
        CLI.info("");
        CLI.info(THIN_SEP);
        CLI.info("  " + title);
        CLI.info(THIN_SEP);
        for (String opt : options) {
            CLI.info("  " + opt);
        }
        CLI.info("  0. Indietro");
        CLI.info(CHOICE);
    }
}