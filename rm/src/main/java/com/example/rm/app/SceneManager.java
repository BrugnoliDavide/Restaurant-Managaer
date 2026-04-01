package com.example.rm.app;

import com.example.rm.preference.SimpleGraphicsManager;
import com.example.rm.view.View;
import com.example.rm.view.ViewFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestisce la navigazione tra le diverse view mantenendo le dimensioni della finestra.
 */
public final class SceneManager {

    private static final Logger logger = Logger.getLogger(SceneManager.class.getName());


    private static final double DEFAULT_WIDTH = 1100.0;
    private static final double DEFAULT_HEIGHT = 720.0;


    private static final double MIN_WIDTH = 800.0;
    private static final double MIN_HEIGHT = 600.0;

    private static Stage primaryStage;
    private static Scene currentScene;
    private static final Map<String, View> viewCache = new HashMap<>();

    private SceneManager() {
        throw new AssertionError("Classe non istanziabile");
    }

    /**
     * Inizializza il SceneManager con lo Stage principale.
     * Deve essere chiamato una sola volta all'avvio dell'applicazione.
     *
     * @param stage lo Stage principale dell'applicazione
     * @throws NullPointerException se stage è null
     */
    public static void init(Stage stage) {
        primaryStage = Objects.requireNonNull(stage, "Lo stage non può essere null");

        configureStage();

        logger.log(Level.INFO, "SceneManager inizializzato correttamente");
    }

    /**
     * Configura le proprietà dello Stage principale.
     */
    private static void configureStage() {
        primaryStage.setWidth(DEFAULT_WIDTH);
        primaryStage.setHeight(DEFAULT_HEIGHT);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setResizable(true);
    }

    /**
     * Mostra la schermata di login.
     */
    public static void showLogin() {
        ensureInitialized();
        setScene(ViewFactory.getLoginView().getRoot());
    }

    /**
     * Mostra la schermata del menu principale.
     */
    public static void showMenu() {
        ensureInitialized();
        setRoot(getOrCreateView("menu").getRoot());
    }

    /**
     * Mostra la schermata finanziaria.
     */
    public static void showFinancial() {
        ensureInitialized();
        logger.log(Level.INFO, "Caricamento vista finanziaria");
        setRoot(getOrCreateView("financial").getRoot());
    }

    /**
     * Mostra la schermata di gestione utenti.
     */
    public static void showUsers() {
        ensureInitialized();
        setRoot(getOrCreateView("users").getRoot());
    }

    /**
     * Mostra la schermata del manager.
     */
    public static void showManager() {
        ensureInitialized();
        setRoot(getOrCreateView("manager").getRoot());
    }

    /**
     * Mostra la schermata del cameriere.
     */
    public static void showWaiter() {
        ensureInitialized();
        setRoot(getOrCreateView("cameriere").getRoot());
    }

    /**
     * Mostra la schermata di presa ordine per un tavolo specifico.
     *
     * @param tavolo il numero del tavolo
     */
    public static void showTakeOrder(int tavolo) {
        ensureInitialized();
        logger.log(Level.INFO, "Apertura presa ordine per tavolo {0}", tavolo);
        setRoot(ViewFactory.forTakeOrder(tavolo).getRoot());
    }

    /**
     * Mostra una view generica basata sul ruolo specificato.
     *
     * @param role il ruolo della view da mostrare
     */
    public static void showView(String role) {
        ensureInitialized();
        Objects.requireNonNull(role, "Il ruolo non può essere null");
        setRoot(getOrCreateView(role).getRoot());
    }

    /**
     * Svuota la cache delle view.
     * Da chiamare quando si cambia modalità UI (es. da standard a e-ink).
     */
    public static void clearViewCache() {
        viewCache.clear();
        logger.log(Level.INFO, "Cache delle view svuotata");
    }

    /**
     * Recupera una view dalla cache o ne crea una nuova se non presente.
     *
     * @param roleKey la chiave del ruolo
     * @return la view corrispondente
     */
    private static View getOrCreateView(String roleKey) {
        String key = cacheKey(roleKey);
        return viewCache.computeIfAbsent(key, k -> {
            logger.log(Level.FINE, "Creazione nuova view per: {0}", roleKey);
            return ViewFactory.forRole(roleKey);
        });
    }

    /**
     * Genera una chiave per la cache basata sul ruolo e sulla modalità UI.
     *
     * @param roleKey il ruolo
     * @return la chiave della cache
     */
    private static String cacheKey(String roleKey) {
        String prefix = SimpleGraphicsManager.isEinkMode() ? "eink:" : "std:";
        return prefix + roleKey.toLowerCase();
    }

    /**
     * Imposta una nuova scena con il root specificato.
     * Utilizzato principalmente per la schermata di login.
     * Preserva le dimensioni correnti della finestra.
     *
     * @param root il nodo root della scena
     */
    private static void setScene(Parent root) {
        Objects.requireNonNull(root, "Il root non può essere null");

        double currentWidth = (currentScene != null) ? currentScene.getWidth() : DEFAULT_WIDTH;
        double currentHeight = (currentScene != null) ? currentScene.getHeight() : DEFAULT_HEIGHT;


        currentScene = new Scene(root, currentWidth, currentHeight);
        primaryStage.setScene(currentScene);

        logger.log(Level.FINE, "Nuova scena impostata con dimensioni: {0}x{1}",
                new Object[]{currentWidth, currentHeight});
    }

    /**
     * Cambia solo il root della scena corrente senza creare una nuova Scene.
     * Questo metodo preserva automaticamente le dimensioni della finestra.
     *
     * @param root il nuovo nodo root
     */
    private static void setRoot(Parent root) {
        Objects.requireNonNull(root, "Il root non può essere null");

        if (currentScene == null) {
            logger.log(Level.WARNING, "currentScene null, creazione nuova scena");
            setScene(root);
            return;
        }

        // Cambia solo il root mantenendo la stessa Scene (e quindi le stesse dimensioni)
        currentScene.setRoot(root);

        logger.log(Level.FINE, "Root della scena aggiornato, dimensioni preservate: {0}x{1}",
                new Object[]{currentScene.getWidth(), currentScene.getHeight()});
    }

    /**
     * Verifica che il SceneManager sia stato inizializzato.
     *
     * @throws IllegalStateException se il SceneManager non è inizializzato
     */
    private static void ensureInitialized() {
        if (primaryStage == null) {
            logger.log(Level.SEVERE, "SceneManager non inizializzato");
            throw new IllegalStateException("SceneManager.init(Stage) deve essere chiamato prima di utilizzare il manager");
        }
    }

    // !! i metodi che seguono possono anche essere deprecati
    /**
     * Restituisce la larghezza predefinita dell'applicazione.
     *
     * @return la larghezza predefinita in pixel
     */
    public static double getDefaultWidth() {
        return DEFAULT_WIDTH;
    }

    /**
     * Restituisce l'altezza predefinita dell'applicazione.
     *
     * @return l'altezza predefinita in pixel
     */
    public static double getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }

    /**
     * @return la larghezza corrente in pixel, o la larghezza predefinita se non inizializzato
     */
    public static double getCurrentWidth() {
        return (currentScene != null) ? currentScene.getWidth() : DEFAULT_WIDTH;
    }

    /**
     * @return l'altezza corrente in pixel, o l'altezza predefinita se non inizializzato
     */
    public static double getCurrentHeight() {
        return (currentScene != null) ? currentScene.getHeight() : DEFAULT_HEIGHT;
    }
}