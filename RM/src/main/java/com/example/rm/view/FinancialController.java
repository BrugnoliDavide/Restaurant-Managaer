package com.example.rm.view;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import com.example.rm.service.LoggerService;
import com.example.rm.view.component.DateHeaderController;
import com.example.rm.view.component.OrderRowController;
import com.example.rm.view.screens.OrderDetailView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller per la vista Financial.
 * Gestisce la visualizzazione degli ordini con filtri e navigazione ai dettagli.
 */
public class FinancialController {

    private static final Logger logger = LoggerService.getLogger(FinancialController.class);

    /* =======================
       COSTANTI
       ======================= */

    private static final String EMPTY_MESSAGE = "Nessun ordine trovato.";
    private static final String ERROR_LOAD_ORDER_MESSAGE = "Errore caricamento ordine";
    private static final String DATE_PATTERN = "EEEE d MMMM yyyy";

    /* =======================
       FXML BINDINGS
       ======================= */

    @FXML private VBox ordersContainer;
    @FXML private Label lblManage;
    @FXML private TextField txtSearch;

    /* =======================
       STATE
       ======================= */

    private List<Order> allOrdersMaster = new ArrayList<>();

    /* =======================
       INITIALIZATION
       ======================= */

    @FXML
    public void initialize() {
        validateFXMLInjections();
        setupEventHandlers();
        loadDataFromDB();
    }

    /**
     * Valida che tutti i componenti FXML siano stati iniettati correttamente.
     */
    private void validateFXMLInjections() {
        if (ordersContainer == null) {
            logger.log(Level.SEVERE, "ordersContainer non iniettato da FXML");
        }
        if (lblManage == null) {
            logger.log(Level.WARNING, "lblManage non iniettato da FXML");
        }
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non iniettato da FXML");
        }
    }

    /**
     * Configura tutti gli event handlers dell'interfaccia.
     */
    private void setupEventHandlers() {
        setupSearchListener();
    }

    /* =======================
       DATA MANAGEMENT
       ======================= */

    /**
     * Carica tutti gli ordini dal database.
     */
    private void loadDataFromDB() {
        try {
            allOrdersMaster = DatabaseService.getAllOrdersWithTotal();
            renderOrders(allOrdersMaster);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento degli ordini", e);
            allOrdersMaster = new ArrayList<>();
            renderOrders(allOrdersMaster);
        }
    }

    /* =======================
       SEARCH & FILTER
       ======================= */

    /**
     * Configura il listener per la ricerca in tempo reale.
     */
    private void setupSearchListener() {
        if (txtSearch == null) {
            logger.log(Level.WARNING, "txtSearch non disponibile per il listener");
            return;
        }

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                filterAndRender(newValue);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore durante il filtraggio degli ordini", e);
            }
        });
    }

    /**
     * Filtra e renderizza gli ordini in base alla query di ricerca.
     * @param query Stringa di ricerca
     */
    private void filterAndRender(String query) {
        String normalizedQuery = normalizeSearchQuery(query);

        if (normalizedQuery.isEmpty()) {
            renderOrders(allOrdersMaster);
            return;
        }

        List<Order> filteredOrders = filterOrders(normalizedQuery);
        renderOrders(filteredOrders);
    }

    /**
     * Normalizza la query di ricerca.
     * @param query Query originale
     * @return Query normalizzata (lowercase, trimmed)
     */
    private String normalizeSearchQuery(String query) {
        return query == null ? "" : query.toLowerCase().trim();
    }

    /**
     * Filtra gli ordini in base alla query.
     * @param query Query normalizzata
     * @return Lista di ordini filtrati
     */
    private List<Order> filterOrders(String query) {
        return allOrdersMaster.stream()
                .filter(order -> matchesSearchQuery(order, query))
                .collect(Collectors.toList());
    }

    /**
     * Verifica se un ordine corrisponde alla query di ricerca.
     * @param order Ordine da verificare
     * @param query Query di ricerca
     * @return true se l'ordine corrisponde
     */
    private boolean matchesSearchQuery(Order order, String query) {
        return matchesOrderId(order, query)
                || matchesTable(order, query)
                || matchesTotal(order, query)
                || matchesDate(order, query);
    }

    /**
     * Verifica se l'ID dell'ordine corrisponde alla query.
     */
    private boolean matchesOrderId(Order order, String query) {
        return String.valueOf(order.getId()).contains(query);
    }

    /**
     * Verifica se il numero tavolo corrisponde alla query.
     */
    private boolean matchesTable(Order order, String query) {
        return String.valueOf(order.getTavolo()).contains(query);
    }

    /**
     * Verifica se il totale corrisponde alla query.
     */
    private boolean matchesTotal(Order order, String query) {
        return String.valueOf(order.getTotale()).contains(query);
    }

    /**
     * Verifica se la data corrisponde alla query.
     */
    private boolean matchesDate(Order order, String query) {
        return order.getDataOra() != null &&
                order.getDataOra().toString().toLowerCase().contains(query);
    }

    /* =======================
       RENDERING
       ======================= */

    /**
     * Renderizza la lista di ordini raggruppati per data.
     * @param ordersToRender Lista di ordini da visualizzare
     */
    private void renderOrders(List<Order> ordersToRender) {
        if (ordersContainer == null) {
            logger.log(Level.SEVERE, "Impossibile renderizzare: ordersContainer è null");
            return;
        }

        ordersContainer.getChildren().clear();

        if (ordersToRender.isEmpty()) {
            renderEmptyState();
            return;
        }

        Map<LocalDate, List<Order>> ordersByDate = groupOrdersByDate(ordersToRender);
        renderOrderGroups(ordersByDate);
    }

    /**
     * Raggruppa gli ordini per data in ordine decrescente.
     * @param orders Lista di ordini
     * @return Map con data come chiave e lista di ordini come valore
     */
    private Map<LocalDate, List<Order>> groupOrdersByDate(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getDataOra().toLocalDate(),
                        () -> new TreeMap<>(Collections.reverseOrder()),
                        Collectors.toList()
                ));
    }

    /**
     * Renderizza i gruppi di ordini per data.
     * @param ordersByDate Map data -> ordini
     */
    private void renderOrderGroups(Map<LocalDate, List<Order>> ordersByDate) {
        DateTimeFormatter headerFormatter = createDateFormatter();

        for (Map.Entry<LocalDate, List<Order>> entry : ordersByDate.entrySet()) {
            String formattedDate = formatDate(entry.getKey(), headerFormatter);
            renderDateSection(formattedDate, entry.getValue());
        }
    }

    /**
     * Crea il formatter per le date.
     * @return DateTimeFormatter configurato
     */
    private DateTimeFormatter createDateFormatter() {
        return DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.ITALY);
    }

    /**
     * Formatta una data.
     * @param date Data da formattare
     * @param formatter Formatter da utilizzare
     * @return Data formattata in maiuscolo
     */
    private String formatDate(LocalDate date, DateTimeFormatter formatter) {
        return date.format(formatter).toUpperCase();
    }

    /**
     * Renderizza una sezione con header data e ordini.
     * @param formattedDate Data formattata
     * @param orders Lista di ordini per quella data
     */
    private void renderDateSection(String formattedDate, List<Order> orders) {
        Parent dateHeader = loadDateHeader(formattedDate);
        if (dateHeader != null) {
            ordersContainer.getChildren().add(dateHeader);
        }

        for (Order order : orders) {
            Parent orderRow = loadOrderRow(order);
            if (orderRow != null) {
                ordersContainer.getChildren().add(orderRow);
            }
        }
    }

    /**
     * Renderizza lo stato vuoto quando non ci sono ordini.
     */
    private void renderEmptyState() {
        Label emptyLabel = new Label(EMPTY_MESSAGE);
        emptyLabel.getStyleClass().add("empty-state-label");
        ordersContainer.getChildren().add(emptyLabel);
    }

    /* =======================
       FXML LOADING
       ======================= */

    /**
     * Carica un header di data da FXML.
     * @param dateText Testo della data da visualizzare
     * @return Parent contenente l'header
     */
    private Parent loadDateHeader(String dateText) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/DateHeader.fxml")
            );
            Parent root = loader.load();

            DateHeaderController controller = loader.getController();
            if (controller != null) {
                controller.setDateText(dateText);
            }

            return root;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento DateHeader.fxml", e);
            return createFallbackDateHeader(dateText);
        }
    }

    /**
     * Crea un header di data di fallback in caso di errore.
     * @param dateText Testo della data
     * @return Label con la data
     */
    private Label createFallbackDateHeader(String dateText) {
        Label label = new Label(dateText);
        label.getStyleClass().add("date-header");
        return label;
    }

    /**
     * Carica una riga ordine da FXML.
     * @param order Ordine da visualizzare
     * @return Parent contenente la riga ordine
     */
    private Parent loadOrderRow(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/OrderRow.fxml")
            );
            Parent root = loader.load();

            OrderRowController controller = loader.getController();
            if (controller != null) {
                controller.setOrder(order, this::navigateToOrderDetail);
            }

            return root;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento OrderRow.fxml per ordine #" +
                    order.getId(), e);
            return createErrorLabel();
        }
    }

    /**
     * Crea una label di errore per ordini non caricabili.
     * @return Label con messaggio di errore
     */
    private Label createErrorLabel() {
        Label errorLabel = new Label(ERROR_LOAD_ORDER_MESSAGE);
        errorLabel.getStyleClass().add("error-label");
        return errorLabel;
    }

    /* =======================
       NAVIGATION
       ======================= */

    /**
     * Naviga alla vista di dettaglio di un ordine.
     * @param order Ordine da visualizzare
     */
    private void navigateToOrderDetail(Order order) {
        if (order == null) {
            logger.log(Level.WARNING, "Tentativo di navigare a dettaglio con ordine null");
            return;
        }

        try {
            logger.log(Level.INFO, "Navigazione a dettaglio ordine #{0}", order.getId());

            OrderDetailView detailView = new OrderDetailView(order);

            if (ordersContainer != null && ordersContainer.getScene() != null) {
                ordersContainer.getScene().setRoot(detailView.getRoot());
            } else {
                logger.log(Level.WARNING, "Scene non disponibile per la navigazione");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante la navigazione al dettaglio ordine #" +
                    order.getId(), e);
        }
    }


    @FXML
    private void goBack() {
        try {
            logger.log(Level.INFO, "Ritorno alla vista Manager");

            View managerView = ViewFactory.forRole("manager");

            if (ordersContainer != null && ordersContainer.getScene() != null) {
                ordersContainer.getScene().setRoot(managerView.getRoot());
            } else {
                logger.log(Level.WARNING, "Scene non disponibile per la navigazione");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il ritorno al manager", e);
        }
    }
}