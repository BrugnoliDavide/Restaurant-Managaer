package rm.view;

import rm.app.UserSession;
import rm.controller.KitchenUseCase;
import rm.model.User;
import rm.preference.KitchenPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitchenControllerTest {

    @BeforeAll
    static void initJavaFx() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");

        try {
            new JFXPanel();
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile inizializzare JavaFX Toolkit", e);
        }
    }

    @BeforeEach
    void setupUserSession() {
        // Crea un utente mock
        User mockUser = mock(User.class);
        when(mockUser.getUsername()).thenReturn("testKitchen");
        when(mockUser.getRole()).thenReturn("kitchen");

        // Usa il metodo di test per inizializzare la sessione
        UserSession.setInstanceForTesting(mockUser);
    }

    @AfterEach
    void cleanup() {
        UserSession.clearInstanceForTesting();

       KitchenController.setKitchenUseCase(null);
    }

    @Test
    void refreshData_whenNoOrders_showsEmptyLabel() throws Exception {
        // Setup mock
        KitchenUseCase uc = mock(KitchenUseCase.class);
        KitchenPreferences prefs = mock(KitchenPreferences.class);

        when(uc.loadPreferences(anyString())).thenReturn(prefs);
        when(prefs.isSplitMixedCategoryOrders()).thenReturn(false);
        when(prefs.isIncludeOtherCategories()).thenReturn(true);
        when(prefs.getSelectedCategories()).thenReturn(new HashSet<>());
        when(uc.loadFilteredOrders(anyString())).thenReturn(Collections.emptyList());

        KitchenController.setKitchenUseCase(uc);

        AtomicReference<VBox> ordersContainerRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            KitchenController controller = new KitchenController();
            setFxFields(controller);

            ordersContainerRef.set((VBox) getField(controller, "ordersContainer"));
            controller.refreshData();
        });

        runOnFxThreadAndWait(() -> {
            VBox ordersContainer = ordersContainerRef.get();

            assertNotNull(ordersContainer, "ordersContainer non deve essere null");
            assertEquals(1, ordersContainer.getChildren().size(),
                    "Deve esserci esattamente un elemento");
            assertTrue(ordersContainer.getChildren().get(0) instanceof Label,
                    "L'elemento deve essere una Label");

            Label emptyLabel = (Label) ordersContainer.getChildren().get(0);
            assertEquals("Nessun ordine in attesa.", emptyLabel.getText(),
                    "Il testo della label deve corrispondere");
        });

        verify(uc, atLeastOnce()).loadPreferences("testKitchen");
        verify(uc, atLeastOnce()).loadFilteredOrders("testKitchen");
        verify(uc, never()).splitMixedOrdersIfNeeded();
    }

    @Test
    void refreshData_whenSplitEnabled_callsSplit() throws Exception {
        KitchenUseCase uc = mock(KitchenUseCase.class);
        KitchenPreferences prefs = mock(KitchenPreferences.class);

        when(uc.loadPreferences(anyString())).thenReturn(prefs);
        when(prefs.isSplitMixedCategoryOrders()).thenReturn(true);
        when(prefs.isIncludeOtherCategories()).thenReturn(true);
        when(prefs.getSelectedCategories()).thenReturn(new HashSet<>());
        when(uc.loadFilteredOrders(anyString())).thenReturn(Collections.emptyList());
        doNothing().when(uc).splitMixedOrdersIfNeeded();

        KitchenController.setKitchenUseCase(uc);

        runOnFxThreadAndWait(() -> {
            KitchenController controller = new KitchenController();
            setFxFields(controller);
            controller.refreshData();
        });

        verify(uc, times(1)).splitMixedOrdersIfNeeded();
        verify(uc, atLeastOnce()).loadPreferences("testKitchen");
        verify(uc, atLeastOnce()).loadFilteredOrders("testKitchen");
    }

    @Test
    void refreshData_whenSplitDisabled_doesNotCallSplit() throws Exception {
        KitchenUseCase uc = mock(KitchenUseCase.class);
        KitchenPreferences prefs = mock(KitchenPreferences.class);

        when(uc.loadPreferences(anyString())).thenReturn(prefs);
        when(prefs.isSplitMixedCategoryOrders()).thenReturn(false);
        when(prefs.isIncludeOtherCategories()).thenReturn(true);
        when(prefs.getSelectedCategories()).thenReturn(new HashSet<>());
        when(uc.loadFilteredOrders(anyString())).thenReturn(Collections.emptyList());

        KitchenController.setKitchenUseCase(uc);

        runOnFxThreadAndWait(() -> {
            KitchenController controller = new KitchenController();
            setFxFields(controller);
            controller.refreshData();
        });

        verify(uc, never()).splitMixedOrdersIfNeeded();
        verify(uc, atLeastOnce()).loadPreferences("testKitchen");
        verify(uc, atLeastOnce()).loadFilteredOrders("testKitchen");
    }

    private static void runOnFxThreadAndWait(Runnable r) throws Exception {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> exception = new AtomicReference<>();

            Platform.runLater(() -> {
                try {
                    r.run();
                } catch (Throwable t) {
                    exception.set(t);
                } finally {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "Timeout nell'esecuzione dell'operazione JavaFX");

            if (exception.get() != null) {
                throw new RuntimeException("Errore nell'esecuzione JavaFX", exception.get());
            }
        }
    }

    private static void setFxFields(KitchenController controller) {
        setField(controller, "ordersContainer", new VBox());
        setField(controller, "profileBtn", new StackPane());
        setField(controller, "profileCircle", new Circle());
        setField(controller, "lblHeaderName", new Label());
        setField(controller, "lblHeaderRole", new Label());
        setField(controller, "lblWelcomeMsg", new Label());
        setField(controller, "lblActiveFilters", new Label());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            fail("Errore impostando il campo " + fieldName + ": " + e.getMessage());
        }
    }

    private static Object getField(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            fail("Errore recuperando il campo " + fieldName + ": " + e.getMessage());
            return null;
        }
    }
}