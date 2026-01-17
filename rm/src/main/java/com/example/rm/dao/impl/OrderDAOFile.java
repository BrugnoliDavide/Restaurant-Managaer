package com.example.rm.dao.impl;

import com.example.rm.dao.OrderTierOneDAO;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;

import java.io.*;
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
public class OrderDAOFile implements OrderTierOneDAO {

    private static final Logger logger = Logger.getLogger(OrderDAOFile.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_SEPARATOR = ";";

    private final Path ordersFilePath;
    private final Path orderItemsDir;
    private int nextOrderId;

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
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length > 0) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        maxId = Math.max(maxId, id);
                    } catch (NumberFormatException e) {
                        logger.log(Level.WARNING, "ID non valido alla riga {0}", i);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore calcolo prossimo ID", e);
        }
        return maxId + 1;
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
        Path itemsFile = orderItemsDir.resolve("order_" + orderId + ".csv");
        StringBuilder content = new StringBuilder("menu_item_id;quantita;prezzo_snapshot;costo_snapshot;nome_snapshot\n");

        for (OrderItem item : items) {
            if (item.getProduct() == null) continue;

            content.append(item.getProduct().getId()).append(CSV_SEPARATOR)
                    .append(item.getQuantita()).append(CSV_SEPARATOR)
                    .append(item.getPrezzoSnapshot()).append(CSV_SEPARATOR)
                    .append(item.getCostoSnapshot()).append(CSV_SEPARATOR)
                    .append(item.getProduct().getNome().replace(CSV_SEPARATOR, ","))
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

                    double totale = calculateOrderTotal(id);

                    orders.add(new Order(id, dataOra, tavolo, username, note, status, totale));
                }
            }

            orders.sort(Comparator.comparing(Order::getDataOra).reversed());

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore recupero ordini", e);
        }

        return orders;
    }

    private double calculateOrderTotal(int orderId) {
        Path itemsFile = orderItemsDir.resolve("order_" + orderId + ".csv");
        if (!Files.exists(itemsFile)) {
            return 0.0;
        }

        try {
            List<String> lines = Files.readAllLines(itemsFile);
            double total = 0.0;

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(CSV_SEPARATOR);
                if (parts.length >= 3) {
                    int quantita = Integer.parseInt(parts[1].trim());
                    double prezzo = Double.parseDouble(parts[2].trim());
                    total += quantita * prezzo;
                }
            }

            return total;

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore calcolo totale ordine {0}", orderId);
            return 0.0;
        }
    }

    @Override
    public List<Order> getOrdersByStatus(String statusTarget) {
        return getAllOrdersWithTotal().stream()
                .filter(order -> order.getStatus().equals(statusTarget))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> getKitchenActiveOrders() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        return getAllOrdersWithTotal().stream()
                .filter(order -> order.getStatus().equals("to-do"))
                .filter(order -> order.getDataOra().isAfter(twentyFourHoursAgo))
                .sorted(Comparator.comparing(Order::getDataOra))
                .collect(Collectors.toList());
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
        Path itemsFile = orderItemsDir.resolve("order_" + orderId + ".csv");

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
        Path itemsFile = orderItemsDir.resolve("order_" + orderId + ".csv");

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
                    double prezzoSnap = Double.parseDouble(parts[2].trim());
                    double costoSnap = Double.parseDouble(parts[3].trim());
                    String nomeSnap = parts[4].trim();

                    MenuProduct product = new MenuProduct();
                    product.setId(menuItemId);
                    product.setNome(nomeSnap);
                    product.setTipologia("Non disponibile");

                    OrderItem item = new OrderItem();
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
        return getOrdersByStatus("delivered");
    }

    @Override
    public boolean markOrderAsDelivered(int orderId) {
        return setOrderStatus(orderId, "delivered");
    }

    @Override
    public boolean markOrderAsPaid(int orderId) {
        return setOrderStatus(orderId, "closed");
    }

    @Override
    public boolean decomposeOrderIfNeeded(int orderId) {
        logger.log(Level.INFO, "Scomposizione ordini non implementata per file system");
        return true;
    }

    @Override
    public boolean hasPendingOrders(int tavolo) {

        return getAllOrdersWithTotal().stream()
                .anyMatch(order -> order.getTavolo() == tavolo &&
                        !order.getStatus().equals("delivered"));
    }
    @Override
    public List<Integer> getPendingOrderIds(int tavolo) {
        return getAllOrdersWithTotal().stream()
                .filter(order -> order.getTavolo() == tavolo &&
                        !order.getStatus().equals("delivered") &&
                        !order.getStatus().equals("canceled"))
                .map(Order::getId)
                .collect(Collectors.toList());
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
}