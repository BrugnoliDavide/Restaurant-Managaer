package com.example.rm.app;

import com.example.rm.controller.*;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.dao.ProductDAO;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.service.ConnectionManager;
import com.example.rm.service.SecurityService;
import com.example.rm.exception.AuthenticationException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interfaccia a riga di comando (CLI) per Restaurant Manager.
 * <p>
 * Consente al <strong>manager</strong> di eseguire le operazioni principali
 * senza avviare l'interfaccia grafica JavaFX:
 * <ul>
 *   <li>Configurazione della connessione al database</li>
 *   <li>Autenticazione</li>
 *   <li>Gestione staff (lista, creazione, eliminazione utenti)</li>
 *   <li>Gestione menù (lista, aggiunta, modifica, eliminazione prodotti)</li>
 *   <li>Consultazione ordini e report finanziario</li>
 *   <li>Cambio password</li>
 * </ul>
 * <p>
 * La classe riutilizza integralmente i layer {@code controller} e {@code service}
 * già presenti nel progetto, senza introdurre dipendenze aggiuntive.
 * <p>
 * <strong>Avvio:</strong> eseguire il metodo {@code main} di questa classe
 * al posto di {@link Launcher#main(String[])}.
 */
public class CliApp {

    // =====================================================================
    //  Costanti
    // =====================================================================

    private static final Logger logger = Logger.getLogger(CliApp.class.getName());

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String SEPARATOR =
            "════════════════════════════════════════════════════════════════";
    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────────────";

    // =====================================================================
    //  Servizi (riuso del layer esistente)
    // =====================================================================

    private static final ManagerUseCase   managerUseCase   = new ManagerService();
    private static final MenuUseCase      menuUseCase      = new MenuService();
    private static final FinancialUseCase financialUseCase = new FinancialService();
    private static final ProductDAO       productDAO       = new DatabaseProductDAO();

    // =====================================================================
    //  Stato della sessione
    // =====================================================================

    private static String loggedUsername = null;
    private static String loggedRole    = null;

    // =====================================================================
    //  Entry point
    // =====================================================================

    public static void main(String[] args) {

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");

        Scanner sc = new Scanner(System.in);
        printBanner();

        // 1. Configurazione DB
        configureDatabase(sc);

        // 2. Login
        if (!login(sc)) {
            System.out.println("\n✖ Autenticazione fallita. Uscita.");
            return;
        }

        // 3. Verifica ruolo manager
        if (!"manager".equalsIgnoreCase(loggedRole)) {
            System.out.println("\n⚠ Questa CLI è riservata al ruolo manager.");
            System.out.println("  Il tuo ruolo corrente è: " + loggedRole);
            System.out.println("  Uscita.");
            return;
        }

        // 4. Menu principale
        mainLoop(sc);
        System.out.println("\nArrivederci!");
    }

    // =====================================================================
    //  Banner
    // =====================================================================

    private static void printBanner() {
        System.out.println(SEPARATOR);
        System.out.println("     RESTAURANT MANAGER — Interfaccia CLI");
        System.out.println(SEPARATOR);
        System.out.println();
    }

    // =====================================================================
    //  Configurazione database
    // =====================================================================

    private static void configureDatabase(Scanner sc) {
        System.out.println("─── Configurazione Database ───\n");

        // Tentativo di caricamento da preferenze salvate
        ConnectionManager.loadFromPreferences();

        if (ConnectionManager.testConnection()) {
            System.out.println("✔ Connessione al database riuscita (configurazione salvata).\n");
            com.example.rm.service.OrderService.usePostgres();
            return;
        }

        System.out.println("Nessuna configurazione valida trovata. Inserire i parametri:\n");

        System.out.print("  Host [localhost]: ");
        String host = sc.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("  Porta [5432]: ");
        String port = sc.nextLine().trim();
        if (port.isEmpty()) port = "5432";

        System.out.print("  Nome database: ");
        String dbName = sc.nextLine().trim();

        System.out.print("  Username DB: ");
        String dbUser = sc.nextLine().trim();

        System.out.print("  Password DB: ");
        String dbPass = sc.nextLine().trim();

        ConnectionManager.configure(host, port, dbName, dbUser, dbPass);

        if (ConnectionManager.testConnection()) {
            System.out.println("\n✔ Connessione al database riuscita.\n");
        } else {
            String err = ConnectionManager.getLastConnectionError();
            System.out.println("\n✖ Connessione fallita: " +
                    (err != null ? err : "errore sconosciuto"));
            configureDatabase(sc);
        }
    }

    // =====================================================================
    //  Login
    // =====================================================================

    private static boolean login(Scanner sc) {
        System.out.println("─── Login ───\n");

        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("  Username: ");
            String user = sc.nextLine().trim();
            System.out.print("  Password: ");
            String pass = sc.nextLine().trim();

            try {
                String role = SecurityService.authenticate(user, pass);
                loggedUsername = user;
                loggedRole = role;
                System.out.println("\n✔ Benvenuto, " + user + " [" + role + "]");
                return true;
            } catch (AuthenticationException e) {
                System.out.println("  ✖ " + e.getUserMessage()
                        + " (tentativo " + attempt + "/3)\n");
            } catch (Exception e) {
                System.out.println("  ✖ Errore imprevisto: " + e.getMessage()
                        + " (tentativo " + attempt + "/3)\n");
            }
        }
        return false;
    }

    // =====================================================================
    //  Loop principale
    // =====================================================================

    private static void mainLoop(Scanner sc) {
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> staffMenu(sc);
                case "2" -> menuMenu(sc);
                case "3" -> ordersMenu(sc);
                case "4" -> changePassword(sc);
                case "0" -> running = false;
                default  -> System.out.println("  ⚠ Scelta non valida.\n");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("  MENU PRINCIPALE — " + loggedUsername);
        System.out.println(SEPARATOR);
        System.out.println("  1. Gestione Staff");
        System.out.println("  2. Gestione Menù");
        System.out.println("  3. Ordini e Report");
        System.out.println("  4. Cambia Password");
        System.out.println("  0. Esci");
        System.out.println(THIN_SEP);
        System.out.print("  Scelta: ");
    }

    // =====================================================================
    //  1. GESTIONE STAFF
    // =====================================================================

    private static void staffMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println(THIN_SEP);
            System.out.println("  GESTIONE STAFF");
            System.out.println(THIN_SEP);
            System.out.println("  1. Elenco utenti");
            System.out.println("  2. Aggiungi utente");
            System.out.println("  3. Elimina utente");
            System.out.println("  0. Indietro");
            System.out.print("  Scelta: ");

            switch (sc.nextLine().trim()) {
                case "1" -> listUsers();
                case "2" -> addUser(sc);
                case "3" -> deleteUser(sc);
                case "0" -> back = true;
                default  -> System.out.println("  ⚠ Scelta non valida.");
            }
        }
    }

    private static void listUsers() {
        List<User> users = managerUseCase.loadAllUsers();
        System.out.println();
        if (users.isEmpty()) {
            System.out.println("  Nessun utente registrato.");
            return;
        }
        System.out.printf("  %-20s %-15s%n", "USERNAME", "RUOLO");
        System.out.println("  " + "─".repeat(35));
        for (User u : users) {
            System.out.printf("  %-20s %-15s%n", u.getUsername(), u.getRole());
        }
        System.out.println("  Totale: " + users.size() + " utenti.");
    }

    private static void addUser(Scanner sc) {
        System.out.println();
        System.out.print("  Username nuovo utente: ");
        String u = sc.nextLine().trim();
        System.out.print("  Password: ");
        String p = sc.nextLine().trim();
        System.out.println("  Ruoli disponibili: manager, cameriere, cucina, cassiere");
        System.out.print("  Ruolo: ");
        String r = sc.nextLine().trim().toLowerCase();

        if (u.isEmpty() || p.isEmpty() || r.isEmpty()) {
            System.out.println("  ✖ Tutti i campi sono obbligatori.");
            return;
        }

        if (!List.of("manager", "cameriere", "cucina", "cassiere").contains(r)) {
            System.out.println("  ✖ Ruolo non valido.");
            return;
        }

        boolean ok = SecurityService.registerUser(u, p, r);
        if (ok) {
            System.out.println("  ✔ Utente «" + u + "» creato con ruolo «" + r + "».");
        } else {
            System.out.println("  ✖ Impossibile creare l'utente (username già esistente?).");
        }
    }

    private static void deleteUser(Scanner sc) {
        listUsers();
        System.out.println();
        System.out.print("  Username da eliminare: ");
        String target = sc.nextLine().trim();

        if (target.isEmpty()) {
            System.out.println("  ✖ Username non fornito.");
            return;
        }
        if (target.equalsIgnoreCase(loggedUsername)) {
            System.out.println("  ✖ Non è possibile eliminare l'utente attualmente collegato.");
            return;
        }

        System.out.print("  Conferma eliminazione di «" + target + "»? (s/n): ");
        if (!"s".equalsIgnoreCase(sc.nextLine().trim())) {
            System.out.println("  Operazione annullata.");
            return;
        }

        boolean ok = managerUseCase.deleteUser(target);
        System.out.println(ok
                ? "  ✔ Utente «" + target + "» eliminato."
                : "  ✖ Utente non trovato o errore durante l'eliminazione.");
    }

    // =====================================================================
    //  2. GESTIONE MENÙ
    // =====================================================================

    private static void menuMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println(THIN_SEP);
            System.out.println("  GESTIONE MENÙ");
            System.out.println(THIN_SEP);
            System.out.println("  1. Elenco prodotti");
            System.out.println("  2. Aggiungi prodotto");
            System.out.println("  3. Modifica prodotto");
            System.out.println("  4. Elimina prodotto");
            System.out.println("  0. Indietro");
            System.out.print("  Scelta: ");

            switch (sc.nextLine().trim()) {
                case "1" -> listProducts();
                case "2" -> addProduct(sc);
                case "3" -> editProduct(sc);
                case "4" -> deleteProduct(sc);
                case "0" -> back = true;
                default  -> System.out.println("  ⚠ Scelta non valida.");
            }
        }
    }

    private static void listProducts() {
        List<MenuProduct> products = menuUseCase.loadAllProducts();
        System.out.println();
        if (products.isEmpty()) {
            System.out.println("  Nessun prodotto nel menù.");
            return;
        }

        System.out.printf("  %-5s %-25s %-15s %10s %10s %8s  %-20s%n",
                "ID", "NOME", "CATEGORIA", "PREZZO", "COSTO", "MARG.%", "ALLERGENI");
        System.out.println("  " + "─".repeat(98));

        for (MenuProduct p : products) {
            System.out.printf("  %-5d %-25s %-15s %10.2f %10.2f %7d%%  %-20s%n",
                    p.getId(),
                    truncate(p.getNome(), 25),
                    truncate(p.getTipologia(), 15),
                    p.getPrezzoVendita(),
                    p.getCostoRealizzazione(),
                    p.getPercentualeMargine(),
                    truncate(p.getAllergeni(), 20));
        }
        System.out.println("  Totale: " + products.size() + " prodotti.");
    }

    private static void addProduct(Scanner sc) {
        System.out.println();
        System.out.println("  ─── Nuovo prodotto ───");

        System.out.print("  Nome: ");
        String nome = sc.nextLine().trim();
        if (nome.isEmpty()) { System.out.println("  ✖ Nome obbligatorio."); return; }

        // Mostra categorie esistenti come suggerimento
        List<String> categorie = menuUseCase.loadCategories();
        if (!categorie.isEmpty()) {
            System.out.println("  Categorie esistenti: " + String.join(", ", categorie));
        }
        System.out.print("  Categoria (tipologia): ");
        String tipologia = sc.nextLine().trim();
        if (tipologia.isEmpty()) { System.out.println("  ✖ Categoria obbligatoria."); return; }

        BigDecimal prezzo = readBigDecimal(sc, "  Prezzo di vendita (€): ");
        if (prezzo == null) return;

        BigDecimal costo = readBigDecimal(sc, "  Costo di realizzazione (€): ");
        if (costo == null) return;

        System.out.print("  Allergeni (vuoto se nessuno): ");
        String allergeni = sc.nextLine().trim();

        MenuProduct product = new MenuProduct(nome, tipologia, prezzo, costo, allergeni);
        boolean ok = menuUseCase.addProduct(product);
        System.out.println(ok
                ? "  ✔ Prodotto «" + nome + "» aggiunto al menù."
                : "  ✖ Errore durante l'inserimento.");
    }

    private static void editProduct(Scanner sc) {
        listProducts();
        System.out.println();
        System.out.print("  ID del prodotto da modificare: ");
        int id = readInt(sc);
        if (id <= 0) { System.out.println("  ✖ ID non valido."); return; }

        MenuProduct existing = menuUseCase.getProductById(id);
        if (existing == null) {
            System.out.println("  ✖ Prodotto con ID " + id + " non trovato.");
            return;
        }

        System.out.println("  (Premere Invio per mantenere il valore attuale)\n");

        System.out.print("  Nome [" + existing.getNome() + "]: ");
        String nome = sc.nextLine().trim();
        if (!nome.isEmpty()) existing.setNome(nome);

        System.out.print("  Categoria [" + existing.getTipologia() + "]: ");
        String tip = sc.nextLine().trim();
        if (!tip.isEmpty()) existing.setTipologia(tip);

        System.out.print("  Prezzo vendita [" + existing.getPrezzoVendita() + "]: ");
        String prezzoStr = sc.nextLine().trim();
        if (!prezzoStr.isEmpty()) {
            try { existing.setPrezzoVendita(new BigDecimal(prezzoStr)); }
            catch (NumberFormatException e) {
                System.out.println("  ✖ Valore non valido."); return;
            }
        }

        System.out.print("  Costo realizzazione [" + existing.getCostoRealizzazione() + "]: ");
        String costoStr = sc.nextLine().trim();
        if (!costoStr.isEmpty()) {
            try { existing.setCostoRealizzazione(new BigDecimal(costoStr)); }
            catch (NumberFormatException e) {
                System.out.println("  ✖ Valore non valido."); return;
            }
        }

        System.out.print("  Allergeni [" + existing.getAllergeni() + "]: ");
        String allerg = sc.nextLine().trim();
        if (!allerg.isEmpty()) existing.setAllergeni(allerg);

        boolean ok = menuUseCase.updateProduct(existing);
        System.out.println(ok
                ? "  ✔ Prodotto aggiornato."
                : "  ✖ Errore durante l'aggiornamento.");
    }

    private static void deleteProduct(Scanner sc) {
        listProducts();
        System.out.println();
        System.out.print("  ID del prodotto da eliminare: ");
        int id = readInt(sc);
        if (id <= 0) { System.out.println("  ✖ ID non valido."); return; }

        MenuProduct existing = menuUseCase.getProductById(id);
        if (existing == null) {
            System.out.println("  ✖ Prodotto con ID " + id + " non trovato.");
            return;
        }

        System.out.print("  Conferma eliminazione di «" + existing.getNome() + "»? (s/n): ");
        if (!"s".equalsIgnoreCase(sc.nextLine().trim())) {
            System.out.println("  Operazione annullata.");
            return;
        }

        boolean ok = productDAO.delete((long) id);
        System.out.println(ok
                ? "  ✔ Prodotto eliminato."
                : "  ✖ Errore durante l'eliminazione.");
    }

    // =====================================================================
    //  3. ORDINI E REPORT
    // =====================================================================

    private static void ordersMenu(Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println(THIN_SEP);
            System.out.println("  ORDINI E REPORT");
            System.out.println(THIN_SEP);
            System.out.println("  1. Tutti gli ordini (con totale)");
            System.out.println("  2. Dettaglio di un ordine");
            System.out.println("  3. Ordini da pagare");
            System.out.println("  4. Ordini attivi in cucina");
            System.out.println("  0. Indietro");
            System.out.print("  Scelta: ");

            switch (sc.nextLine().trim()) {
                case "1" -> listAllOrders();
                case "2" -> orderDetail(sc);
                case "3" -> listOrdersToPay();
                case "4" -> listKitchenActive();
                case "0" -> back = true;
                default  -> System.out.println("  ⚠ Scelta non valida.");
            }
        }
    }

    private static void listAllOrders() {
        List<Order> orders = financialUseCase.loadAllOrdersWithTotal();
        printOrderTable(orders, "TUTTI GLI ORDINI");
    }

    private static void listOrdersToPay() {
        List<Order> orders = com.example.rm.service.OrderService.getToPay();
        printOrderTable(orders, "ORDINI DA PAGARE");
    }

    private static void listKitchenActive() {
        List<Order> orders = com.example.rm.service.OrderService.getKitchenActive();
        printOrderTable(orders, "ORDINI ATTIVI IN CUCINA");
    }

    private static void printOrderTable(List<Order> orders, String title) {
        System.out.println();
        System.out.println("  " + title);
        System.out.println("  " + "─".repeat(title.length()));

        if (orders.isEmpty()) {
            System.out.println("  Nessun ordine trovato.");
            return;
        }

        System.out.printf("  %-6s %-18s %-8s %-15s %-12s %10s%n",
                "ID", "DATA/ORA", "TAVOLO", "CAMERIERE", "STATO", "TOTALE €");
        System.out.println("  " + "─".repeat(73));

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Order o : orders) {
            System.out.printf("  %-6d %-18s %-8d %-15s %-12s %10.2f%n",
                    o.getId(),
                    o.getDataOra().format(DTF),
                    o.getTavolo(),
                    truncate(o.getUsername(), 15),
                    o.getStatus(),
                    o.getTotale());
            grandTotal = grandTotal.add(o.getTotale());
        }
        System.out.println("  " + "─".repeat(73));
        System.out.printf("  %d ordini — Totale complessivo: €%.2f%n",
                orders.size(), grandTotal);
    }

    private static void orderDetail(Scanner sc) {
        System.out.println();
        System.out.print("  ID ordine: ");
        int id = readInt(sc);
        if (id <= 0) { System.out.println("  ✖ ID non valido."); return; }

        Order order = com.example.rm.service.OrderService.findById(id);
        if (order == null) {
            System.out.println("  ✖ Ordine #" + id + " non trovato.");
            return;
        }

        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.printf("  ║  ORDINE #%-33d║%n", order.getId());
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf("  ║  Data:      %-30s║%n", order.getDataOra().format(DTF));
        System.out.printf("  ║  Tavolo:    %-30d║%n", order.getTavolo());
        System.out.printf("  ║  Cameriere: %-30s║%n", order.getUsername());
        System.out.printf("  ║  Stato:     %-30s║%n", order.getStatus());
        System.out.printf("  ║  Totale:    €%-29.2f║%n", order.getTotale());
        if (order.hasNote()) {
            System.out.printf("  ║  Note:      %-30s║%n", truncate(order.getNote(), 30));
        }
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  ARTICOLI                                ║");
        System.out.println("  ╠══════════════════════════════════════════╣");

        List<OrderItem> items =
                com.example.rm.service.OrderService.getItemsDetailed(id);

        if (items.isEmpty()) {
            System.out.println("  ║  (nessun articolo)                       ║");
        } else {
            for (OrderItem item : items) {
                String line = String.format("%dx %s — €%.2f",
                        item.getQuantita(),
                        item.getDisplayName(),
                        item.getPrezzoSnapshot()
                                .multiply(BigDecimal.valueOf(item.getQuantita())));
                System.out.printf("  ║  • %-39s║%n", truncate(line, 39));
            }
        }
        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // =====================================================================
    //  4. CAMBIO PASSWORD
    // =====================================================================

    private static void changePassword(Scanner sc) {
        System.out.println();
        System.out.println("  ─── Cambio Password ───");
        System.out.print("  Password attuale: ");
        String current = sc.nextLine().trim();
        System.out.print("  Nuova password (min 6 caratteri): ");
        String newPass = sc.nextLine().trim();
        System.out.print("  Conferma nuova password: ");
        String confirm = sc.nextLine().trim();

        if (newPass.length() < 6) {
            System.out.println("  ✖ La password deve essere di almeno 6 caratteri.");
            return;
        }

        if (!newPass.equals(confirm)) {
            System.out.println("  ✖ Le password non corrispondono.");
            return;
        }

        boolean ok = SecurityService.changePassword(loggedUsername, current, newPass);
        System.out.println(ok
                ? "  ✔ Password aggiornata con successo."
                : "  ✖ Password attuale errata o errore durante l'aggiornamento.");
    }

    // =====================================================================
    //  Utilità
    // =====================================================================

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static BigDecimal readBigDecimal(Scanner sc, String prompt) {
        System.out.print(prompt);
        String raw = sc.nextLine().trim().replace(",", ".");
        try {
            BigDecimal val = new BigDecimal(raw);
            if (val.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("  ✖ Il valore non può essere negativo.");
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            System.out.println("  ✖ Valore numerico non valido.");
            return null;
        }
    }

    private static int readInt(Scanner sc) {
        String raw = sc.nextLine().trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}