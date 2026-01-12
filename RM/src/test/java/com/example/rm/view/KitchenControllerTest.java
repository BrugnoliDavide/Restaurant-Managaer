package com.example.rm.view;

import com.example.rm.controller.KitchenUseCase;
import com.example.rm.preference.KitchenPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitchenControllerTest {

    @BeforeAll
    static void initJavaFx() {
        // Avvia il toolkit JavaFX una volta sola
        new JFXPanel();
    }

    @Test
    void refreshData_whenNoOrders_showsEmptyLabel() throws Exception {
        KitchenUseCase uc = mock(KitchenUseCase.class);
        KitchenPreferences prefs = mock(KitchenPreferences.class);

        when(uc.loadPreferences("guest")).thenReturn(prefs);
        when(prefs.isSplitMixedCategoryOrders()).thenReturn(false);
        when(prefs.isIncludeOtherCategories()).thenReturn(true);
        when(uc.loadFilteredOrders("guest")).thenReturn(Collections.emptyList());

        // NB: richiede che tu abbia aggiunto KitchenController.setKitchenUseCase(...)
        KitchenController.setKitchenUseCase(uc); // [file:182]

        KitchenController controller = new KitchenController();
        setFxFields(controller);

        runOnFxThread(controller::refreshData);

        VBox ordersContainer = (VBox) getField(controller, "ordersContainer"); // [file:182]
        assertEquals(1, ordersContainer.getChildren().size());
        assertTrue(ordersContainer.getChildren().get(0) instanceof Label);
        assertEquals("Nessun ordine in attesa.", ((Label) ordersContainer.getChildren().get(0)).getText());

        verify(uc).loadPreferences("guest");
        verify(uc).loadFilteredOrders("guest");
        verify(uc, never()).splitMixedOrdersIfNeeded();
    }

    @Test
    void refreshData_whenSplitEnabled_callsSplit() throws Exception {
        KitchenUseCase uc = mock(KitchenUseCase.class);
        KitchenPreferences prefs = mock(KitchenPreferences.class);

        when(uc.loadPreferences("guest")).thenReturn(prefs);
        when(prefs.isSplitMixedCategoryOrders()).thenReturn(true);
        when(prefs.isIncludeOtherCategories()).thenReturn(true);
        when(uc.loadFilteredOrders("guest")).thenReturn(Collections.emptyList());

        KitchenController.setKitchenUseCase(uc); // [file:182]

        KitchenController controller = new KitchenController();
        setFxFields(controller);

        runOnFxThread(controller::refreshData);

        verify(uc).splitMixedOrdersIfNeeded();
    }

    // ----- helpers -----

    private static void runOnFxThread(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Timeout JavaFX thread");
    }

    private static void setFxFields(KitchenController controller) {
        setField(controller, "ordersContainer", new VBox());      // [file:182]
        setField(controller, "profileBtn", new StackPane());      // [file:182]
        setField(controller, "profileCircle", new Circle());      // [file:182]
        setField(controller, "lblHeaderName", new Label());       // [file:182]
        setField(controller, "lblHeaderRole", new Label());       // [file:182]
        setField(controller, "lblWelcomeMsg", new Label());       // [file:182]
        setField(controller, "lblActiveFilters", new Label());    // [file:182]
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field: " + fieldName, e);
        }
    }

    private static Object getField(Object target, String fieldName) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field: " + fieldName, e);
        }
    }
}
