package com.example.demo.service;

import com.example.demo.model.MenuProduct;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.demo.app.UsersFactory;

public class DatabaseService {

    // DATI DI CONNESSIONE (Quelli messi nel docker-compose)
    private static final String URL = "jdbc:postgresql://localhost:5432/restaurant_db";
    private static final String USER = "admin";
    private static final String PASS = "password123";

    // 1. Metodo per scaricare TUTTI i prodotti dal DB vero
    public static List<MenuProduct> getAllProducts() {
        List<MenuProduct> prodotti = new ArrayList<>();

        // Query SQL
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni FROM menu_items";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Scorriamo le righe della tabella risultante
            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String tipologia = rs.getString("tipologia");
                double prezzo = rs.getDouble("prezzo_vendita");
                double costo = rs.getDouble("costo_realizzazione");
                String allergeni = rs.getString("allergeni");

                // Creiamo l'oggetto Java e lo aggiungiamo alla lista
                prodotti.add(new MenuProduct(id, nome, tipologia, prezzo, costo, allergeni));
            }

        } catch (SQLException e) {
            System.out.println("ERRORE DATABASE: " + e.getMessage());
            e.printStackTrace();
        }

        return prodotti;
    }

    // 2. Metodo per raggruppare per categorie (Logica Java)
    public static Map<String, List<MenuProduct>> getMenuByCategories() {
        List<MenuProduct> tuttiIProdotti = getAllProducts();
        return tuttiIProdotti.stream()
                .collect(Collectors.groupingBy(MenuProduct::getTipologia));
    }

    // 3. Metodo Quantità (PER ORA FITTIZIO)
    // Dato che non abbiamo ancora creato la tabella 'vendite' su Docker,
    // restituiamo 0 per evitare errori. Lo implementeremo nel prossimo step.

    // Metodo per inserire un nuovo prodotto
    public static boolean addProduct(MenuProduct p) {
        String sql = "INSERT INTO menu_items (nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Riempiamo i punti interrogativi (?) con i dati
            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getTipologia());
            pstmt.setDouble(3, p.getPrezzoVendita());
            pstmt.setDouble(4, p.getCostoRealizzazione());
            pstmt.setString(5, p.getAllergeni());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Se è > 0, ha funzionato

        } catch (SQLException e) {
            System.out.println("ERRORE INSERIMENTO: " + e.getMessage());
            return false; // Qualcosa è andato storto (es. nome duplicato)
        }
    }

    // 1. AGGIORNAMENTO (Richiede un oggetto con ID valido)
    public static boolean updateProduct(MenuProduct p) {
        String sql = "UPDATE menu_items SET nome = ?, tipologia = ?, prezzo_vendita = ?, costo_realizzazione = ?, allergeni = ? WHERE id = ?";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getTipologia());
            pstmt.setDouble(3, p.getPrezzoVendita());
            pstmt.setDouble(4, p.getCostoRealizzazione());
            pstmt.setString(5, p.getAllergeni());
            pstmt.setInt(6, p.getId()); // FONDAMENTALE: Usa l'ID per trovare la riga

            return pstmt.executeUpdate() > 0;

        } catch (java.sql.SQLException e) {
            System.out.println("ERRORE UPDATE: " + e.getMessage());
            return false;
        }
    }

    // 2. ELIMINAZIONE
// Metodo ELIMINAZIONE con DEBUG
    public static boolean deleteProduct(int id) {
        System.out.println("--- INIZIO TENTATIVO ELIMINAZIONE ---");
        System.out.println("ID ricevuto da eliminare: " + id);

        if (id <= 0) {
            System.out.println("ERRORE: L'ID non è valido (è 0 o minore). Impossibile eliminare dal DB.");
            return false;
        }

        String sql = "DELETE FROM menu_items WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            // Eseguiamo
            int rowsAffected = pstmt.executeUpdate();

            System.out.println("Righe eliminate realmente dal DB: " + rowsAffected);

            if (rowsAffected > 0) {
                System.out.println("SUCCESSO: Prodotto eliminato.");
                return true;
            } else {
                System.out.println("FALLIMENTO: Nessuna riga trovata con questo ID. Il prodotto esiste nel DB?");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("ERRORE SQL GRAVE: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    // Metodo per ottenere la lista delle categorie esistenti
    public static List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        // DISTINCT serve a non avere duplicati (es. se ho 10 Primi, voglio la scritta "Primi" una volta sola)
        String sql = "SELECT DISTINCT tipologia FROM menu_items ORDER BY tipologia";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(rs.getString("tipologia"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Se il DB è vuoto, restituiamo almeno delle categorie base per non lasciare l'utente smarrito
        if (categories.isEmpty()) {
            categories.add("Primi");
            categories.add("Secondi");
            categories.add("Bibite");
        }

        return categories;
    }




    // Metodo Transazionale per creare Ordine + Righe
    public static boolean createOrder(List<OrderItem> items, Integer tavolo, String note) {
        if (items.isEmpty()) return false;

        String sqlOrder = "INSERT INTO orders (username, tavolo, note, status) VALUES (?,?, ?, ?)";
        String sqlItem = "INSERT INTO order_items (order_id, menu_item_id, quantita, prezzo_vendita_snapshot, costo_realizzazione_snapshot) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmtOrder = null;
        PreparedStatement pstmtItem = null;

        try {
            conn = DriverManager.getConnection(URL, USER, PASS);

            // 1. DISABILITIAMO L'AUTO-COMMIT (Inizia la transazione manuale)
            conn.setAutoCommit(false);

            // --- A. INSERIMENTO TESTATA ORDINE ---
            // Return_Generated_Keys ci serve per sapere l'ID dell'ordine appena creato (es. Ordine #50)
            pstmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
            pstmtOrder.setString(1, "Manager"); // Username fisso per ora
            if (tavolo != null) pstmtOrder.setInt(2, tavolo);
            else pstmtOrder.setNull(2, java.sql.Types.INTEGER);
            pstmtOrder.setString(3, note);


            pstmtOrder.setString(4, "ordered");

            int affectedRows = pstmtOrder.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creazione ordine fallita.");

            // Recuperiamo l'ID generato
            int orderId = 0;
            try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Fallito recupero ID ordine.");
                }
            }

            // --- B. INSERIMENTO RIGHE (ITEMS) ---
            pstmtItem = conn.prepareStatement(sqlItem);

            for (OrderItem item : items) {
                pstmtItem.setInt(1, orderId); // Usiamo l'ID appena recuperato
                pstmtItem.setInt(2, item.getProduct().getId()); // ID dal Menu
                pstmtItem.setInt(3, item.getQuantita());
                pstmtItem.setDouble(4, item.getPrezzoSnapshot()); // Prezzo congelato
                pstmtItem.setDouble(5, item.getCostoSnapshot());  // Costo congelato

                // Aggiungiamo al "batch" (mucchio di query da eseguire insieme)
                pstmtItem.addBatch();
            }

            // Eseguiamo tutte le righe insieme
            pstmtItem.executeBatch();

            // 2. COMMIT (Se siamo arrivati qui, salviamo tutto definitivamente)
            conn.commit();
            System.out.println("Ordine #" + orderId + " creato con successo con " + items.size() + " righe.");
            return true;

        } catch (SQLException e) {
            // 3. ROLLBACK (Se c'è un errore, annulliamo tutto come se non fosse mai successo)
            if (conn != null) {
                try {
                    System.out.println("Errore rilevato! Annullamento operazione (Rollback)...");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            // Chiudiamo tutto
            try { if (pstmtItem != null) pstmtItem.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmtOrder != null) pstmtOrder.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }





    // Metodo aggiornato per contare le vendite usando la nuova tabella
    public static long getQuantitySold(String nomeProdotto) {
        // JOIN tra order_items e menu_items per trovare le righe giuste tramite il nome
        String sql = "SELECT SUM(oi.quantita) FROM order_items oi " +
                "JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE mi.nome = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeProdotto);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1); // Ritorna la somma (es. 127)
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

// --- GESTIONE STATI ORDINE ---

    // 1. Cambia lo stato di un ordine (es. da APERTO a CHIUSO)
    public static boolean setOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);

            return pstmt.executeUpdate() > 0;

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Ottieni ordini filtrati per stato (es. getOrders("APERTO"))
    public static java.util.List<Order> getOrdersByStatus(String targetStatus) {
        java.util.List<Order> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY data_ora DESC";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, targetStatus);
            java.sql.ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp("data_ora").toLocalDateTime(),
                        rs.getInt("tavolo"),
                        rs.getString("username"),
                        rs.getString("note"),
                        rs.getString("status"),
                        rs.getDouble("totale")
                ));
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    public static java.util.List<com.example.demo.model.Order> getAllOrdersWithTotal() {
        java.util.List<com.example.demo.model.Order> list = new java.util.ArrayList<>();

        // Query Magica: Unisce ordini e articoli, somma i prezzi (prezzo * quantità) e raggruppa
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) as totale_calcolato " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora DESC"; // Ordine cronologico inverso (più recenti prima)

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASS);
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new com.example.demo.model.Order(
                        rs.getInt("id"),
                        rs.getTimestamp("data_ora").toLocalDateTime(),
                        rs.getInt("tavolo"),
                        rs.getString("username"),
                        rs.getString("note"),
                        rs.getString("status"),
                        rs.getDouble("totale_calcolato") // Leggiamo la somma calcolata
                ));
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

// quary ottimizzate rispetto a getbystatus

    public static List<Order> getKitchenActiveOrders() {
        List<Order> list = new ArrayList<>();

        // QUERY OTTIMIZZATA (PostgreSQL):
        // 1. Seleziona ordini e calcola totale (COALESCE serve se non ci sono righe)
        // 2. Filtra per stato = 'ordinato'
        // 3. Filtra per data_ora >= adesso - 24 ore
        String sql = "SELECT o.id, o.data_ora, o.tavolo, o.username, o.note, o.status, " +
                "COALESCE(SUM(oi.quantita * oi.prezzo_vendita_snapshot), 0) as totale_calcolato " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "WHERE o.status = ? " +
                "AND o.data_ora >= NOW() - INTERVAL '24 HOURS' " +
                "GROUP BY o.id, o.data_ora, o.tavolo, o.username, o.note, o.status " +
                "ORDER BY o.data_ora ASC"; // In cucina di solito si vuole prima il più vecchio (FIFO)

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Impostiamo lo stato che cerchiamo
            pstmt.setString(1, "ordered");

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getTimestamp("data_ora").toLocalDateTime(),
                        rs.getInt("tavolo"),
                        rs.getString("username"),
                        rs.getString("note"),
                        rs.getString("status"),
                        rs.getDouble("totale_calcolato")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }



    // --- METODO PER I DETTAGLI (CUCINA) ---
    public static List<String> getOrderItemsForDisplay(int orderId) {
        List<String> details = new ArrayList<>();
        // Uniamo order_items con menu_items per avere il nome del piatto
        String sql = "SELECT mi.nome, oi.quantita " +
                "FROM order_items oi " +
                "JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                "WHERE oi.order_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String nome = rs.getString("nome");
                int qta = rs.getInt("quantita");
                // Formattiamo la stringa: "2x Carbonara"
                details.add(qta + "x " + nome);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }

    public static List<com.example.demo.model.User> getAllUsers() {
        List<com.example.demo.model.User> list = new java.util.ArrayList<>();
        String sql = "SELECT username, role FROM users ORDER BY role, username";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                String u = rs.getString("username");
                String r = rs.getString("role");

                // --- CORREZIONE QUI ---
                // Non usiamo più 'new User(...)', ma la Factory!
                // La Factory deciderà se creare un ManagerUser, WaiterUser, ecc.
                com.example.demo.model.User userObj = com.example.demo.app.UsersFactory.createUser(u, r);

                if (userObj != null) {
                    list.add(userObj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    // --- CANCELLAZIONE UTENTE ---
    public static boolean deleteUser(String usernameToDelete) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameToDelete);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Utente '" + usernameToDelete + "' eliminato correttamente.");
                return true;
            } else {
                System.out.println("Nessun utente trovato con username: " + usernameToDelete);
                return false;
            }

        } catch (java.sql.SQLException e) {
            System.err.println("Errore durante l'eliminazione dell'utente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }




}







