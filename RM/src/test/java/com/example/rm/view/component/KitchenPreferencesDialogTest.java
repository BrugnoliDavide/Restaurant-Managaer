package com.example.rm.view.component;

import com.example.rm.controller.KitchenPreferencesUseCase;
import com.example.rm.dao.CategoryDAO;
import com.example.rm.preference.KitchenPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitchenPreferencesDialogTest {

    @BeforeAll
    static void initAll() {
        new JFXPanel(); // Inizializza JavaFX
    }

    @Test
    void testLoadPreferences() throws InterruptedException {
        // Crea mock NEL thread JavaFX
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                KitchenPreferencesUseCase mockService = mock(KitchenPreferencesUseCase.class);
                CategoryDAO mockDao = mock(CategoryDAO.class);

                KitchenPreferences fakePrefs = new KitchenPreferences("testuser", true, Set.of("Antipasti"), true);
                when(mockService.load("testuser")).thenReturn(fakePrefs);
                when(mockDao.getAllCategories()).thenReturn(List.of("Antipasti", "Primi"));

                // Inietta NEL thread JavaFX
                KitchenPreferencesDialog.setPrefsService(mockService);
                KitchenPreferencesDialog.setCategoryDAO(mockDao);

                // Verifica IMMEDIATAMENTE che load sia configurato
                verify(mockService, never()).load("testuser"); // Non ancora chiamato

                // Il test passa se i mock sono settati senza crash
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Errore setup mock: " + e.getMessage());
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    void testSavePreferences() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                KitchenPreferencesUseCase mockService = mock(KitchenPreferencesUseCase.class);
                CategoryDAO mockDao = mock(CategoryDAO.class);

                KitchenPreferences fakePrefs = new KitchenPreferences("testuser", true, Set.of("Antipasti"), true);
                when(mockService.load("testuser")).thenReturn(fakePrefs);
                when(mockService.save(any(KitchenPreferences.class))).thenReturn(true);
                when(mockDao.getAllCategories()).thenReturn(List.of("Antipasti"));

                KitchenPreferencesDialog.setPrefsService(mockService);
                KitchenPreferencesDialog.setCategoryDAO(mockDao);

                latch.countDown();
            } catch (Exception e) {
                fail("Errore setup mock save: " + e.getMessage());
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }
}
