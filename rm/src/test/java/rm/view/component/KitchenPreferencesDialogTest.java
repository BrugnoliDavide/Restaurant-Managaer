package rm.view.component;

import rm.controller.KitchenPreferencesUseCase;
import rm.dao.CategoryDAO;
import rm.preference.KitchenPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KitchenPreferencesDialogTest {

    @BeforeAll
    static void initAll() {
        // Configurazione pulita per Xvfb
        System.setProperty("java.awt.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");

        try {
            new JFXPanel();
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile inizializzare JavaFX Toolkit", e);
        }
    }
    private KitchenPreferencesDialog controller;
    private KitchenPreferencesUseCase mockService;
    private CategoryDAO mockDao;
    private Stage mockStage;

    @BeforeEach
    void setUp() throws Exception {
        mockService = mock(KitchenPreferencesUseCase.class);
        mockDao = mock(CategoryDAO.class);
        mockStage = mock(Stage.class);

        // Usiamo il costruttore di default e poi iniettiamo i campi
        controller = new KitchenPreferencesDialog();

        runOnFxThread(() -> {
            try {
                // Setup dipendenze statiche (metodi forniti dalla tua classe)
                KitchenPreferencesDialog.setPrefsService(mockService);
                KitchenPreferencesDialog.setCategoryDAO(mockDao);

                // Setup campi istanza privati tramite Reflection
                setField(controller, "stage", mockStage);
                setField(controller, "username", "testuser");
                setField(controller, "onComplete", null); // Opzionale

                // Setup UI Components (Necessari altrimenti handleSave lancia NullPointerException)
                CheckBox chkSplit = new CheckBox();
                chkSplit.setSelected(true);
                setField(controller, "chkSplitOrders", chkSplit);

                VBox vbox = new VBox();
                // Aggiungiamo una checkbox simulata nella UI
                CheckBox catChk = new CheckBox("Antipasti");
                catChk.setSelected(true);
                vbox.getChildren().add(catChk);
                setField(controller, "vboxCategories", vbox);

                // Setup Preferences iniziali
                KitchenPreferences initialPrefs = new KitchenPreferences("testuser", false, Set.of(), true);
                setField(controller, "currentPreferences", initialPrefs);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testHandleSave_AttemptsToSave() throws Exception {
        // SETUP: Configura il mock per ritornare FALSE.
        // Se ritornasse TRUE -> showSuccessAlert() -> showAndWait() -> BLOCCO INFINITO/POPUP
        when(mockService.save(any(KitchenPreferences.class))).thenReturn(false);

        runOnFxThread(() -> {
            try {
                // Invoca handleSave via Reflection (perché è private/FXML)
                invokeMethod(controller, "handleSave");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // VERIFICA: Controlliamo che abbia provato a salvare
        verify(mockService, times(1)).save(any(KitchenPreferences.class));

        // Verifica che NON abbia chiuso lo stage (perché abbiamo simulato errore)
        verify(mockStage, never()).close();
    }


    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } catch (Exception e) { e.printStackTrace(); }
            finally { latch.countDown(); }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) throw new RuntimeException("Timeout FX");
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void invokeMethod(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }
}