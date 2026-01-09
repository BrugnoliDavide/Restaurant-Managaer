package com.example.rm.view.component;

import com.example.rm.controller.KitchenPreferencesUseCase;
import com.example.rm.preference.KitchenPreferences;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class KitchenPreferencesDialogTest {

    private static KitchenPreferencesUseCase mockService;

    @BeforeAll
    static void initAll() {
        Platform.startup(() -> {});
    }

    @Test
    void testLoadPreferences() throws InterruptedException {
        mockService = mock(KitchenPreferencesUseCase.class);
        KitchenPreferences fakePrefs = new KitchenPreferences("testuser", true, Set.of("Antipasti"), true);
        when(mockService.load("testuser")).thenReturn(fakePrefs);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            KitchenPreferencesDialog.show(null, "testuser", success -> {
                verify(mockService).load("testuser");
                latch.countDown();
            });
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSavePreferences() throws InterruptedException {
        mockService = mock(KitchenPreferencesUseCase.class);
        when(mockService.save(any())).thenReturn(true);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            KitchenPreferencesDialog.show(null, "testuser", success -> {
                verify(mockService).save(any(KitchenPreferences.class));
                latch.countDown();
            });
        });

        Thread.sleep(1000);
        latch.countDown();
        verify(mockService).save(any());
    }
}
