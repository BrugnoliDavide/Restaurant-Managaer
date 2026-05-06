package com.example.rm.dao.impl;

import com.example.rm.dao.OrderDAO;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementazione file system del DAO per gli ordini.
 * Salva gli ordini in formato CSV su file system.
 *
 * Struttura directory:
 * - orders.csv: dati principali ordini
 * - order_items/: directory contenente i file degli articoli per ciascun ordine
 */
public class OrderDAOFile implements OrderDAO {

    private static final Logger logger = Logger.getLogger(OrderDAOFile.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_SEPARATOR = ";";

    private final Path ordersFilePath;
    private final Path orderItemsDir;
    private int nextOrderId;
    private String orderString = "order_";
    private String deliveredString = "delivered";
    
    /**
     * Costruttore con specifica del percorso base
     * @param basePath Percorso della directory base per i file
     */
    public OrderDAOFile(String basePath) throws IOException {
        Path base = Paths.get(basePath);
        Files.createDirectories(base);

        this.ordersFilePath = base.resolve("orders.csv");
        this.orderItemsDir = base.resolve("order_items");
        Files.createDirectories(orderItemsDir);

        initializeFiles();
        this.nextOrderId = calculateNextOrderId();
    }

    private void initializeFiles() throws IOException {
        if (!Files.exists(ordersFilePath)) {
            String header = "id;data_ora;tavolo;username;note;status";
            Files.writeString(ordersFilePath, header + System.lineSeparator());
        }
    }

    private synchronized int calculateNextOrderId() {
        int maxId = 0;
        try {
            List<String> lines = Files.readAllLines(ordersFilePath);
            for (int i = 1; i < lines.size(); i++) {
                maxId = updateMaxIdFromLine(maxId, lines.get(i), i);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore calcolo prossimo ID", e);
        }
        return maxId + 1;
    }

    private int updateMaxIdFromLine(int currentMaxId, String line, int lineIndex) {
        String[] parts = line.split(CSV_SEPARATOR);
        if (parts.length == 0) {
            return currentMaxId;
        }

        try {
            int id = Integer.parseInt(parts[0].trim());
            return Math.max(currentMaxId, id);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "ID non valido alla riga {0}", lineIndex);
            return currentMaxId;
        }
    }


    @Override
    public synchronized boolean createOrder(List<OrderItem> items, Integer tavolo, String note, User utente) {
        if (utente == null || items == null || items.isEmpty()) {
            logger.warning("Tentativo di creare ordine non valido");
            return false;
        }

        int orderId = nextOrderId++;
        String dataOra = LocalDateTime.now().format(DATE_FORMATTER);
        String tavoloStr = tavolo != null ? String.valueOf(tavolo) : "";
        String noteStr = note != null ? note.replace(CSV_SEPARATOR, ",") : "";

        String orderLine = String.join(CSV_SEPARATOR,
                String.valueOf(orderId),
                dataOra,
                tavoloStr,
                utente.getUsername(),
                noteStr,
                "to-do"
        );

        try {
            Files.writeString(ordersFilePath, orderLine + System.lineSeparator(),
                    StandardOpenOption.APPEND);

            saveOrderItems(orderId, items);

            logger.log(Level.INFO, "Ordine #{0} creato con successo", orderId);
            return true;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore creazione ordine", e);
            return false;
        }
    }

    private void saveOrderItems(int orderId, List<OrderItem> items) throws IOException {
        Path itemsFile = orderItemsDir.resolve(orderString + orderId + ".csv");
        StringBuilder content = new StringBuilder(
                "menu_item_id;quantita;prezzo_snapshot;costo_snapshot;nome_snapshot;status\n");

        for (OrderItem item : items) {
            if (item.getProduct() == null) continue;

            content.append(item.getProduct().getId()).append(CSV_SEPARATOR)
                    .append(item.getQuantita()).append(CSV_SEPARATOR)
                    .append(item.getPrezzoSnapshot()).append(CSV_SEPARATOR)
                    .append(item.getCostoSnapshot()).append(CSV_SEPARATOR)
                    .append(item.getProduct().getNome().replace(CSV_SEPARATOR, ","))
                    .append(CSV_SEPARATOR)
                    .append("active")
                    .append(System.lineSeparator());
        }

        Files.writeString(itemsFile, content.toString());
    }

    @Override
    public List<Order> getAllOrdersWithTotal() {
        List<Order> orders = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(ordersFilePath);

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length >= 6) {
                    int id = Integer.parseInt(parts[0].trim());
                    LocalDateTime dataOra = LocalDateTime.parse(parts[1].trim(), DATE_FORMATTER);
                    Integer tavolo = parts[2].trim().isEmpty() ? null : Integer.parseInt(parts[2].trim());
                    String username = parts[3].trim();
                    String note = parts[4].trim();
                    String status = parts[5].trim();

                    BigDecimal totale = calculateOrderTotal(id);

                    orders.add(new Order(id, dataOra, tavolo, username, note, status, totale));
                }
            }

            orders.sort(Comparator.comparing(Order::getDataOra).reversed());

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini", e);
        }

        return orders;
    }

    private BigDecimal calculateOrderTotal(int orderId) {
        Path itemsFile = orderItemsDir.resolve(orderString + orderId + ".csv");
        if (!Files.exists(itemsFile)) {
            return BigDecimal.ZERO;
        }

        try {
            List<String> lines = Files.readAllLines(itemsFile);
            BigDecimal total = BigDecimal.ZERO;

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length >= 3) {
                    int quantita = Integer.parseInt(parts[1].trim());
                    BigDecimal prezzo = new BigDecimal(parts[2].trim());
                    total = total.add(prezzo.multiply(BigDecimal.valueOf(quantita)));
                }
            }

            return total;

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore calcolo totale ordine {0}", orderId);
            return BigDecimal.ZERO;
        }
    }
    @Override
    public List<Order> getOrdersByStatus(String statusTarget) {
        return getAllOrdersWithTotal().stream()
                .filter(order -> order.getStatus().equals(statusTarget))
                .toList();
    }

    @Override
    public List<Order> getKitchenActiveOrders() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        return getAllOrdersWithTotal().stream()
                .filter(order -> order.getStatus().equals("to-do"))
                .filter(order -> order.getDataOra().isAfter(twentyFourHoursAgo))
                .sorted(Comparator.comparing(Order::getDataOra))
                .toList();
    }

    @Override
    public synchronized boolean setOrderStatus(int orderId, String newStatus) {
        try {
            List<String> lines = Files.readAllLines(ordersFilePath);
            boolean found = false;

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length >= 6 && Integer.parseInt(parts[0].trim()) == orderId) {
                    parts[5] = newStatus;
                    lines.set(i, String.join(CSV_SEPARATOR, parts));
                    found = true;
                    break;
                }
            }

            if (found) {
                Files.write(ordersFilePath, lines);
                logger.log(Level.INFO, "Stato ordine {0} aggiornato a {1}", new Object[]{orderId, newStatus});
                return true;
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore aggiornamento stato ordine", e);
        }

        return false;
    }

    @Override
    public List<String> getOrderItemsForDisplay(int orderId) {
        List<String> details = new ArrayList<>();
        Path itemsFile = orderItemsDir.resolve(orderString + orderId + ".csv");

        if (!Files.exists(itemsFile)) {
            return details;
        }

        try {
            List<String> lines = Files.readAllLines(itemsFile);

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length >= 5) {
                    int quantita = Integer.parseInt(parts[1].trim());
                    String nome = parts[4].trim();
                    details.add(quantita + "x " + nome);
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore lettura articoli ordine {0}", orderId);
        }

        return details;
    }

    @Override
    public List<OrderItem> getOrderItemsDetailed(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        Path itemsFile = orderItemsDir.resolve(orderString + orderId + ".csv");

        if (!Files.exists(itemsFile)) {
            return items;
        }

        try {
            List<String> lines = Files.readAllLines(itemsFile);

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length >= 5) {
                    int menuItemId = Integer.parseInt(parts[0].trim());
                    int quantita = Integer.parseInt(parts[1].trim());
                    BigDecimal prezzoSnap = new BigDecimal(parts[2].trim());
                    BigDecimal costoSnap = new BigDecimal(parts[3].trim());
                    String nomeSnap = parts[4].trim();

                    MenuProduct product = new MenuProduct();
                    product.setId(menuItemId);
                    product.setNome(nomeSnap);
                    product.setTipologia("Non disponibile");

                    OrderItem item = new OrderItem();
                    item.setId(i - 1);
                    item.setProduct(product);
                    item.setQuantita(quantita);
                    item.setPrezzoSnapshot(prezzoSnap);
                    item.setCostoSnapshot(costoSnap);
                    item.setNomeSnapshot(nomeSnap);

                    items.add(item);
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore recupero dettagli articoli ordine {0}", orderId);
        }

        return items;
    }

    @Override
    public List<Order> getReadyOrdersForWaiter() {
        return getOrdersByStatus("ready");
    }

    @Override
    public List<Order> getOrdersToPay() {
        return getOrdersByStatus(deliveredString);
    }

    @Override
    public boolean markOrderAsDelivered(int orderId) {
        return setOrderStatus(orderId, deliveredString);
    }

    @Override
    public boolean markOrderAsPaid(int orderId) {
        return setOrderStatus(orderId, "closed");
    }

    @Override
    public synchronized boolean decomposeOrderIfNeeded(int orderId) {
        List<OrderItem> allItems = getOrderItemsDetailed(orderId);
        if (allItems.isEmpty()) return true;

        // Raggruppa per categoria (tipologia)
        Map<String, List<OrderItem>> byCategory = new HashMap<>();
        for (OrderItem item : allItems) {
            String cat = item.getProduct().getTipologia();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
        }

        if (byCategory.size() <= 1) return true;  // già mono-categoria, niente da fare

        // Leggi metadati ordine originale
        Order original = findById(orderId);
        if (original == null) return false;

        // Crea un ordine per ogni categoria
        for (List<OrderItem> categoryItems : byCategory.values()) {
            // Riusa createOrder con un User fittizio (username dal original)
            com.example.rm.model.User fakeUser = new com.example.rm.model.ManagerUser(
                    original.getUsername());
            createOrder(categoryItems, original.getTavolo(), original.getNote(), fakeUser);
        }

        // Elimina ordine originale: sovrascrivi il file rimuovendo la riga
        return deleteOrderById(orderId);
    }

    // metodo privato di supporto
    private synchronized boolean deleteOrderById(int orderId) {
        try {
            List<String> lines = Files.readAllLines(ordersFilePath);
            List<String> updated = new ArrayList<>();
            updated.add(lines.get(0)); // header
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length > 0 && Integer.parseInt(parts[0].trim()) != orderId) {
                    updated.add(lines.get(i));
                }
            }
            Files.write(ordersFilePath, updated);
            // elimina anche il file degli articoli
            Path itemsFile = orderItemsDir.resolve(orderString + orderId + ".csv");
            Files.deleteIfExists(itemsFile);
            return true;
        } catch (IOException | NumberFormatException e) {
            logger.log(Level.SEVERE, "Errore eliminazione ordine {0} da file", orderId);
            return false;
        }
    }

    @Override
    public boolean hasPendingOrders(int tavolo) {

        return getAllOrdersWithTotal().stream()
                .anyMatch(order -> order.getTavolo() == tavolo &&
                        !order.getStatus().equals(deliveredString));
    }
    @Override
    public List<Integer> getPendingOrderIds(int tavolo) {
        return getAllOrdersWithTotal().stream()
                .filter(order -> order.getTavolo() == tavolo &&
                        !order.getStatus().equals(deliveredString) &&
                        !order.getStatus().equals("canceled"))
                .map(Order::getId)
                .toList();
    }

    @Override
    public long getQuantitySoldInDateRange(int productId, LocalDateTime start, LocalDateTime end) {
        long total = 0;

        List<Order> ordersInRange = getAllOrdersWithTotal().stream()
                .filter(order -> !order.getStatus().equals("canceled"))
                .filter(order -> !order.getDataOra().isBefore(start) && !order.getDataOra().isAfter(end))
                .toList();

        for (Order order : ordersInRange) {
            List<OrderItem> items = getOrderItemsDetailed(order.getId());
            for (OrderItem item : items) {
                if (item.getProduct().getId() == productId) {
                    total += item.getQuantita();
                }
            }
        }

        return total;
    }


    @Override
    public Map<Integer, List<String>> getAllOrderItemsForDisplay() {
        Map<Integer, List<String>> result = new HashMap<>();
        List<Order> orders = getAllOrdersWithTotal();
        for (Order order : orders) {
            result.put(order.getId(), getOrderItemsForDisplay(order.getId()));
        }
        return result;
    }


    @Override
    public Set<Integer> getTablesWithPendingOrders() {
        return getAllOrdersWithTotal().stream()
                .filter(o -> !o.getStatus().equals("delivered") && !o.getStatus().equals("closed"))
                .map(Order::getTavolo)
                .collect(Collectors.toSet());
    }

    @Override
    public Order findById(int orderId) {
        return getAllOrdersWithTotal().stream()
                .filter(o -> o.getId() == orderId)
                .findFirst()
                .orElse(null);
    }





    //lo scopo di questa implementazione è prettamente "didattico" e necessario
    //al fine di avere due implementazioni di un DAO, anche se la sua esistenza potrebbe essere di dubbia utilità
    @Override
    public synchronized boolean removeOrderItem(int orderId, int itemId) {
        if (itemId < 0) {
            logger.log(Level.WARNING, "itemId non valido: {0}", itemId);
            return false;
        }

        Path itemsFile = orderItemsDir.resolve(orderString + orderId + ".csv");

        if (!Files.exists(itemsFile)) {
            logger.log(Level.WARNING,
                    "File articoli non trovato per ordine {0}", orderId);
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(itemsFile);

            // La riga 0 è l'header; le righe dati partono dall'indice 1.
            // Per convenzione itemId == indice 0-based della riga dati,
            // quindi la riga nel file è (itemId + 1).
            int targetLine = itemId + 1;

            if (targetLine >= lines.size()) {
                logger.log(Level.WARNING,
                        "itemId {0} fuori range per ordine {1} (righe dati {2})",
                        new Object[]{itemId, orderId, lines.size() - 1});
                return false;
            }

            List<String> updated = new ArrayList<>(lines.size() - 1);
            updated.add(lines.get(0)); // header preservato
            for (int i = 1; i < lines.size(); i++) {
                if (i != targetLine) {
                    updated.add(lines.get(i));
                }
            }

            // Riscrittura sicura: scrivo su file temporaneo e poi rimpiazzo
            // il file originale, così da evitare file troncati in caso di crash.
            Path tmp = itemsFile.resolveSibling(itemsFile.getFileName() + ".tmp");
            Files.write(tmp, updated,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            Files.move(tmp, itemsFile, StandardCopyOption.REPLACE_EXISTING);

            logger.log(Level.INFO,
                    "Rimosso item {0} dall''ordine {1}",
                    new Object[]{itemId, orderId});
            return true;

        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Errore rimozione item {0}   da ordine {1}", new Object[]{itemId,orderId});
            return false;
        }
    }

}