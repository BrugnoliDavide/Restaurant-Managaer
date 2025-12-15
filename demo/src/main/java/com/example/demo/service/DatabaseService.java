package com.example.demo.service;

import com.example.demo.model.MenuProduct;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.example.demo.view.LoginController.logger;


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

            logger.log(Level.SEVERE, "Errore durante il caricamento del file", e);
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

            logger.log(Level.SEVERE, "ERRORE INSERIMENTO", e);

            return false; // Qualcosa è andato storto
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

            logger.log(Level.SEVERE, "ERRORE UPDATE", e);

            return false;
        }
    }

    // 2. ELIMINAZIONE
// Metodo ELIMINAZIONE con DEBUG
    public static boolean deleteProduct(int id) {

        logger.info("TENTATIVO ELIMINAZIONE: \"ID ricevuto da eliminare: \" + id");


        if (id <= 0) {

            //ripeto l'id anche se a tutti gli effetti questo viene già stampato tramite l'istruzione sopra
            logger.log(Level.SEVERE, ": L'ID non è valido (è 0 o minore). Impossibile eliminare dal DB. id:", id );

            return false;
        }

        String sql = "DELETE FROM menu_items WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();

            logger.info("Righe eliminate realmente dal DB: " + rowsAffected);

            if (rowsAffected > 0) {

                logger.info("SUCCESSO: Prodotto eliminato.");

                return true;
            } else {

                logger.warning("\"FALLIMENTO: Nessuna riga trovata con questo ID, id:" + id);

                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE SQL GRAVE: ", e);
            return false;
        }
    }



    public static List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT tipologia FROM menu_items ORDER BY tipologia";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(rs.getString("tipologia"));
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nell'ottenere tutte le categorie ", e);
        }

        // Se il DB è vuoto, si restituiscono almeno delle categorie base per non lasciare l'utente smarrito
        if (categories.isEmpty()) {
            categories.add("Primi");
            categories.add("Secondi");
            categories.add("Bibite");
        }

        return categories;
    }



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
                pstmtItem.setDouble(4, item.getPrezzoSnapshot()); // Prezzo congelato al momento dell'acquisto
                pstmtItem.setDouble(5, item.getCostoSnapshot());  // Costo congelato al momento dell'acquisto

                // Aggiungiamo al batch
                pstmtItem.addBatch();
            }


            pstmtItem.executeBatch();

            // 2. COMMIT (Se siamo arrivati qui, salviamo tutto definitivamente)
            conn.commit();

            logger.info("Ordine #" + orderId + " creato con successo con " + items.size() + " righe.");
            return true;

        } catch (SQLException e) {
            // 3. ROLLBACK (Se c'è un errore, annulliamo tutto come se non fosse mai successo)
            if (conn != null) {
                try {

                    logger.log(Level.SEVERE, "Errore rilevato! Annullamento operazione (Rollback)...");
                    conn.rollback();

                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Si è verificato un errore nell'interazione col Database", ex);
                }
            }

            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nella creazione dell'ordine", e);

            return false;
        } finally {




            //Chiusura primo PreparedStatement
            try {
                if (pstmtItem != null) pstmtItem.close();
            } catch (SQLException e) {
                //WARNING perché è un errore di pulizia, non bloccante
                logger.log(Level.WARNING, "Impossibile chiudere pstmtItem", e);
            }

            //Chiusura secondo PreparedStatement
            try {
                if (pstmtOrder != null) pstmtOrder.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Impossibile chiudere pstmtOrder", e);
            }

            //Chiudi la Connessione (e ripristina autoCommit se serve per il pool)
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Importante se usi connection pooling
                    conn.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Impossibile chiudere o resettare la connessione DB", e);
            }}
    }




    public static long getQuantitySold(String nomeProdotto) {
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
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel prelevare la quantità venduta", e);
        }
        return 0;
    }

    public static boolean setOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);

            return pstmt.executeUpdate() > 0;

        } catch (java.sql.SQLException e) {
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nell'impostare le quantità vendute", e);
            return false;
        }
    }


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
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel prelevare gli ordini a partire dallo stato", e);
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
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel prelevare  tutti gli ordini con totale", e);
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
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel prelevare gli ordini attivi della cucina", e);
        }
        return list;
    }


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
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel prelevare gli ordini", e);

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
            logger.log(Level.SEVERE, "Si è verificato un errore imprevisto nel prelevare gli utenti", e);
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


                logger.log(Level.INFO, "Utente '" + usernameToDelete + "' eliminato correttamente.");

                return true;
            } else {

                logger.log(Level.WARNING, "Nessun utente trovato con username: " + usernameToDelete);
                return false;
            }

        } catch (java.sql.SQLException e) {

            logger.log(Level.SEVERE, "Errore durante l'eliminazione dell'utente: ",e);
            return false;
        }
    }




}







