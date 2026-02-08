package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import com.example.rm.service.ProductLoadingService;
import com.example.rm.view.component.OrderReviewDialog;
import com.example.rm.view.component.ProductRowFactory;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.example.rm.controller.MenuUseCase;
import com.example.rm.controller.OrderUseCase;
import com.example.rm.controller.MenuService;
import com.example.rm.controller.OrderService;
import com.example.rm.app.SceneManager;
import javafx.util.Duration;


//Gestisce il carrello e l'invio degli ordini al database.
public class TakeOrderController {

    private static final Logger logger = Logger.getLogger(TakeOrderController.class.getName());

    @FXML private Label lblTitle;
    @FXML private VBox productsContainer;
    @FXML private Button btnSend;

    private int numeroTavolo;

    private final Map<Integer, OrderItem> carrello = new HashMap<>();
    private static final MenuUseCase menuUseCase = new MenuService();
    private static final OrderUseCase orderUseCase = new OrderService();
    private static final User currentUser = UserSession.getInstance().getUser();

    private static final String ERRORESTRING = "ERRORE: ";

      private ProductLoadingService productLoadingService;
      private ProgressIndicator loadingIndicator;
      private VBox loadingOverlay;

    @FXML
    private TextField searchField;
    private final ObjectProperty<String> searchText = new SimpleObjectProperty<>("");
    private ScheduledExecutorService searchExecutor;
    private List<MenuProduct> allProductsCache = new CopyOnWriteArrayList<>();


    public void init(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
        updateTitle();
        initLoadingIndicator();
        initProductLoadingService();
        setupSearch();
        loadProducts();
        updateSendButton();

    }

    private void initProductLoadingService() {
        if (productLoadingService == null) {
            productLoadingService = new ProductLoadingService(menuUseCase);

            // Configura handler per successo
            productLoadingService.setOnSucceeded(event -> {
                List<MenuProduct> products = productLoadingService.getValue();
                Platform.runLater(() -> {
                    updateProductList(products);
                    showLoadingIndicator(false);
                    logger.info("Prodotti caricati: " + products.size());
                });
            });

            // Configura handler per errore
            productLoadingService.setOnFailed(event -> {
                Throwable ex = productLoadingService.getException();
                Platform.runLater(() -> {
                    showLoadingIndicator(false);
                    showErrorAlert("Errore", "Impossibile caricare i prodotti: " +
                            (ex != null ? ex.getMessage() : "Errore sconosciuto"));
                    logger.log(Level.SEVERE, "Caricamento prodotti fallito", ex);
                });
            });

            // Reset al completamento
            productLoadingService.setOnCancelled(event -> Platform.runLater(() -> showLoadingIndicator(false)));


            logger.info("ProductLoadingService inizializzato");
        }
    }

    private void initLoadingIndicator() {
        createLoadingIndicator();
        createLoadingOverlay();
        addLoadingIndicatorToLayout();
    }

    private void createLoadingIndicator() {
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setProgress(-1); // Indeterminato
        loadingIndicator.setMaxSize(50, 50);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
    }

    private void createLoadingOverlay() {
        loadingOverlay = new VBox(loadingIndicator);
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7);");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
    }

    private void addLoadingIndicatorToLayout() {
        Platform.runLater(() -> {
            if (productsContainer != null) {
                Parent parent = findStackPaneParent(productsContainer);

                if (parent instanceof StackPane stackPane) {
                    addToStackPane(stackPane);
                } else {
                    addFallbackOverlay();
                }
            }
        });
    }

    private Parent findStackPaneParent(Parent node) {
        Parent parent = node.getParent();
        while (parent != null && !(parent instanceof StackPane)) {
            parent = parent.getParent();
        }
        return parent;
    }

    private void addToStackPane(StackPane stackPane) {
        stackPane.getChildren().add(loadingOverlay);
        StackPane.setAlignment(loadingOverlay, Pos.CENTER);
        logger.info("Loading indicator aggiunto correttamente");
    }

    private void addFallbackOverlay() {
        if (productsContainer.getScene() != null) {
            StackPane fallbackPane = new StackPane();
            fallbackPane.getChildren().addAll(productsContainer, loadingOverlay);

            if (productsContainer.getParent() instanceof Pane currentParent) {
                int index = currentParent.getChildren().indexOf(productsContainer);
                currentParent.getChildren().set(index, fallbackPane);
            }
        }
    }
    private void setupSearch() {
        PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(event -> performSearch());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchText.set(newVal);
            searchDebounce.playFromStart();
        });

        // Inizializza executor per ricerca
        searchExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("Search-Thread-" + count.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );

        // Salva cache dei prodotti quando vengono caricati
        // VERIFICA CHE productLoadingService NON SIA NULL
        if (productLoadingService != null) {
            productLoadingService.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    allProductsCache.clear();
                    allProductsCache.addAll(newVal);
                    logger.info("Cache prodotti aggiornata: " + newVal.size() + " prodotti");
                }
            });
        } else {
            // Log di avvertimento e programma il listener per più tardi
            logger.warning("productLoadingService è null durante setupSearch");
            Platform.runLater(() -> {
                if (productLoadingService != null) {
                    productLoadingService.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            allProductsCache.clear();
                            allProductsCache.addAll(newVal);
                            logger.info("Cache prodotti aggiornata (ritardata): " + newVal.size());
                        }
                    });
                } else {
                    logger.severe("productLoadingService ancora null dopo Platform.runLater");
                }
            });
        }
    }



    private void updateTitle() {
        if (lblTitle != null) {
            lblTitle.setText("Select item (Tav. " + numeroTavolo + ")");
        }
    }

    private void loadProducts() {

        loadProductsAsync();
    }

    private void renderProducts(List<MenuProduct> prodotti) {
        productsContainer.getChildren().clear();

        for (MenuProduct prodotto : prodotti) {
            HBox productRow = createProductRow(prodotto);
            productsContainer.getChildren().add(productRow);
        }
    }

    private HBox createProductRow(MenuProduct prodotto) {
        int currentQuantity = getCurrentQuantity(prodotto.getId());
        String formattedPrice = formatPrice(prodotto.getPrezzoVendita());





        return ProductRowFactory.row(
                prodotto.getNome(),
                prodotto.getTipologia(),
                formattedPrice,
                currentQuantity,
                () -> handleIncrementProduct(prodotto),
                () -> handleDecrementProduct(prodotto)
        );
    }

    private int getCurrentQuantity(int productId) {
        return carrello.containsKey(productId)
                ? carrello.get(productId).getQuantita()
                : 0;
    }

    private String formatPrice(double price) {
        return "€ " + String.format("%.2f", price);
    }

    private void handleIncrementProduct(MenuProduct prodotto) {
        incrementProductQuantity(prodotto);
        loadProducts();
    }

    private void handleDecrementProduct(MenuProduct prodotto) {
        decrementProductQuantity(prodotto);
        loadProducts();
    }

    private void incrementProductQuantity(MenuProduct prodotto) {
        if (carrello.containsKey(prodotto.getId())) {
            // Prodotto già presente, incrementa quantità
            OrderItem existingItem = carrello.get(prodotto.getId());
            OrderItem updatedItem = createOrderItem(
                    prodotto,
                    existingItem.getQuantita() + 1
            );
            carrello.put(prodotto.getId(), updatedItem);
        } else {
            // Nuovo prodotto
            OrderItem newItem = createOrderItem(prodotto, 1);
            carrello.put(prodotto.getId(), newItem);
        }

        updateSendButton();

        logger.log(Level.INFO, "Prodotto aggiunto: {0} (Quantità: {1})",
                new Object[]{prodotto.getNome(), carrello.get(prodotto.getId()).getQuantita()});
    }

    private void decrementProductQuantity(MenuProduct prodotto) {
        if (!carrello.containsKey(prodotto.getId())) {
            return;
        }

        OrderItem existingItem = carrello.get(prodotto.getId());
        int currentQuantity = existingItem.getQuantita();

        if (currentQuantity > 1) {
            // Decrementa quantità
            OrderItem updatedItem = createOrderItem(prodotto, currentQuantity - 1);
            carrello.put(prodotto.getId(), updatedItem);
        } else {
            // Rimuovi dal carrello
            carrello.remove(prodotto.getId());
        }

        updateSendButton();

        logger.log(Level.INFO, "Prodotto rimosso: {0}", prodotto.getNome());
    }

    private OrderItem createOrderItem(MenuProduct prodotto, int quantita) {
        return new OrderItem(
                prodotto,
                quantita,
                prodotto.getPrezzoVendita(),      // prezzoSnapshot
                prodotto.getCostoRealizzazione(), // costoSnapshot
                prodotto.getNome()                // nomeSnapshot
        );
    }

    private void clearCart() {
        carrello.clear();
        updateSendButton();
    }

    private void updateSendButton() {
        if (btnSend == null) {
            return;
        }

        int totalItems = calculateTotalItems();
        btnSend.setText("Send Order (" + totalItems + ")");
        btnSend.setDisable(totalItems == 0);
    }

    private int calculateTotalItems() {
        return carrello.values().stream()
                .mapToInt(OrderItem::getQuantita)
                .sum();
    }
    @FXML
    private void handleCancel() {
        try {
            SceneManager.showWaiter();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante l'annullamento", e);
        }
    }

    @FXML
    private void handleSend() {
        if (carrello.isEmpty()) {
            showInfoAlert("Carrello vuoto", "Seleziona almeno un prodotto.");
            return;
        }
        btnSend.setDisable(true);

        // E-ink: invio diretto, niente dialog di review
        if (com.example.rm.preference.SimpleGraphicsManager.isEinkMode()) {
            List<OrderItem> items = new ArrayList<>(carrello.values());
            sendOrderToDatabase(items, ""); // oppure una note predefinita / nulla
            return;
        }


        showOrderReviewDialog();
    }

    private void showOrderReviewDialog() {
        try {
            Stage stage = getStage();
            if (stage == null) {
                logger.log(Level.WARNING, "Stage non disponibile per il dialog");
                return;
            }

            List<OrderItem> items = new ArrayList<>(carrello.values());

            OrderReviewDialog.show(stage, numeroTavolo, items, "", result -> {
                if (result.isConfirmed()) {
                    sendOrderToDatabase(items, result.getNote());

                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore apertura dialog review", e);
            showErrorAlert(ERRORESTRING, "Impossibile aprire il dialog di conferma");
        }
    }

    private void sendOrderToDatabase(List<OrderItem> items, String note) {
        try {
            boolean success = orderUseCase.createOrder(
                    items,
                    numeroTavolo,
                    note,
                    currentUser
            );

            if (success) {
                handleOrderSuccess();
            } else {
                handleOrderFailure();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore invio ordine al database", e);
            showErrorAlert(ERRORESTRING, "Errore durante l'invio dell'ordine: " + e.getMessage());
        }
    }

    private void handleOrderSuccess() {
        logger.log(Level.INFO, "Ordine inviato con successo per tavolo {0}", numeroTavolo);
        showInfoAlert("Successo", "Ordine inviato con successo!");
        clearCart();
        SceneManager.showWaiter();
    }

    private void handleOrderFailure() {
        logger.log(Level.SEVERE, "Impossibile inviare ordine per tavolo {0}", numeroTavolo);
        showErrorAlert(ERRORESTRING, "Impossibile inviare l'ordine al database.");
    }

    private Stage getStage() {
        if (btnSend == null || btnSend.getScene() == null) {
            return null;
        }
        return (Stage) btnSend.getScene().getWindow();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }



    private void loadProductsAsync() {
        // Controlla che l'indicatore sia inizializzato
        if (loadingIndicator == null) {
            logger.warning("Loading indicator non inizializzato, caricamento sincrono");
            loadProductsSync();
            return;
        }

        // Controlla che il service sia stato inizializzato
        if (productLoadingService == null) {
            logger.severe("ProductLoadingService non inizializzato!");
            loadProductsSync();
            return;
        }

        // Mostra l'indicatore di caricamento
        showLoadingIndicator(true);

        // Riavvia se già completato
        if (productLoadingService.getState() == Worker.State.READY ||
                productLoadingService.getState() == Worker.State.SUCCEEDED ||
                productLoadingService.getState() == Worker.State.FAILED) {
            productLoadingService.reset();
        }

        // Avvia il caricamento
        productLoadingService.start();
    }

    private void showLoadingIndicator(boolean show) {
          if (loadingIndicator == null || loadingOverlay == null) {
              logger.warning("Tentativo di mostrare loading indicator non inizializzato");
              return;
          }

          loadingIndicator.setVisible(show);
          loadingOverlay.setVisible(show);

          // Gestisci lo spazio solo quando visibile
          if (show) {
              loadingIndicator.setManaged(true);
              loadingOverlay.setManaged(true);
              loadingIndicator.setProgress(-1);
          } else {
              loadingIndicator.setManaged(false);
              loadingOverlay.setManaged(false);
          }
      }

    private void updateProductList(List<MenuProduct> products) {
          productsContainer.getChildren().clear();

          if (products.isEmpty()) {
              Label emptyLabel = new Label("Nessun prodotto disponibile");
              emptyLabel.getStyleClass().add("empty-state");
              productsContainer.getChildren().add(emptyLabel);
              return;
          }

          // Raggruppa prodotti per categoria
          Map<String, List<MenuProduct>> productsByCategory = products.stream()
                  .collect(Collectors.groupingBy(MenuProduct::getTipologia));

          for (Map.Entry<String, List<MenuProduct>> entry : productsByCategory.entrySet()) {
              // Aggiungi header categoria
              Label categoryLabel = new Label(entry.getKey().toUpperCase());
              categoryLabel.getStyleClass().add("category-header");
              productsContainer.getChildren().add(categoryLabel);

              // Aggiungi prodotti
              for (MenuProduct product : entry.getValue()) {
                  HBox productRow = createProductRow(product);
                  productsContainer.getChildren().add(productRow);
              }

              // Aggiungi separatore
              productsContainer.getChildren().add(new Separator());
          }
    }

    private void loadProductsSync() {
          try {
              List<MenuProduct> prodotti = menuUseCase.loadAllProducts();

              allProductsCache.clear();
              allProductsCache.addAll(prodotti);
              renderProducts(prodotti);
          } catch (Exception e) {
              logger.log(Level.SEVERE, "Errore caricamento prodotti", e);
              showErrorAlert("Errore", "Impossibile caricare i prodotti");
          }
      }

    private void performSearch() {
        String query = searchText.get();

        if (query == null || query.trim().isEmpty()) {
            Platform.runLater(() -> {
                if (!allProductsCache.isEmpty()) {
                    updateProductList(allProductsCache);
                    searchField.setStyle("");
                    searchField.setTooltip(null);
                }
            });
            return;
        }

        final String finalQuery = query.trim().toLowerCase();

        Task<List<MenuProduct>> searchTask = new Task<>() {
            @Override
            protected List<MenuProduct> call() {
                try {
                    return allProductsCache.stream()
                            .filter(product -> matchesSearch(product, finalQuery))
                            .toList();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore durante la ricerca per query: " + finalQuery, e);
                    throw new RuntimeException("Errore durante la ricerca di prodotti per: '" + finalQuery + "'", e);
                }
            }
        };

        searchTask.setOnSucceeded(event -> {
            List<MenuProduct> results = searchTask.getValue();
            Platform.runLater(() -> {
                updateProductList(results);
                showSearchStatus(results.size(), finalQuery);


                if (!results.isEmpty()) {
                    FadeTransition fade = new FadeTransition(Duration.millis(200), productsContainer);
                    fade.setFromValue(0.5);
                    fade.setToValue(1.0);
                    fade.play();
                }
            });
        });

        searchTask.setOnFailed(event ->
            Platform.runLater(() -> {
                searchField.setStyle("-fx-border-color: #e74c3c;");
                searchField.setTooltip(new Tooltip("Errore durante la ricerca"));
                logger.log(Level.SEVERE, "Task ricerca fallito", searchTask.getException());
            })
        );

        searchExecutor.execute(searchTask);
    }


    private boolean matchesSearch(MenuProduct product, String query) {
        if (query.isEmpty()) return true;

        String lowerQuery = query.toLowerCase();
        return product.getNome().toLowerCase().contains(lowerQuery)
                || product.getTipologia().toLowerCase().contains(lowerQuery)
                || (product.getAllergeni() != null && product.getAllergeni().toLowerCase().contains(lowerQuery));
    }
    
    private void showSearchStatus(int resultCount, String query) {
        // Mostra badge con numero risultati
        if (resultCount == 0) {
            searchField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1;");
            searchField.setTooltip(new Tooltip("Nessun prodotto trovato per: " + query));
        } else {
            searchField.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 1;");
            searchField.setTooltip(new Tooltip(resultCount + " prodotti trovati"));
        }
    }

    // Metodo per pulire le risorse
    public void cleanup() {
        if (searchExecutor != null && !searchExecutor.isShutdown()) {
            searchExecutor.shutdownNow();
        }
        if (productLoadingService != null) {
            productLoadingService.cancel();
        }
    }
  }