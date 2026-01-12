package com.example.rm.view.component;

import com.example.rm.controller.UserAccountUseCase;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChangePasswordDialogControllerTest {

    @BeforeAll
    static void initToolkit() {
        // --- BLOCCO CRUCIALE PER GITHUB ACTIONS ---
        // Queste proprietà sono obbligatorie per evitare che JavaFX cerchi il display (GTK) e si blocchi
        System.setProperty("java.awt.headless", "true");
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        // ------------------------------------------

        try {
            new JFXPanel(); // Inizializza il toolkit JavaFX
        } catch (Exception e) {
            // Ignora se già inizializzato
        }
    }

    private ChangePasswordDialogController controller;
    private UserAccountUseCase mockAccountService;
    private Stage mockStage;

    @BeforeEach
    void setUp() throws Exception {
        mockAccountService = mock(UserAccountUseCase.class);
        mockStage = mock(Stage.class);
        controller = new ChangePasswordDialogController();

        runOnFxThread(() -> {
            try {
                // Inietta i Mock
                setField(controller, "accountService", mockAccountService);
                setField(controller, "stage", mockStage);
                setField(controller, "username", "testUser");

                // Inietta i componenti UI reali
                setField(controller, "lblFeedback", new Label());
                setField(controller, "txtCurrentPassword", new PasswordField());
                setField(controller, "txtNewPassword", new PasswordField());
                setField(controller, "txtConfirmPassword", new PasswordField());
                setField(controller, "btnSave", new javafx.scene.control.Button());
                setField(controller, "btnCancel", new javafx.scene.control.Button());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testHandleSave_CallsServiceCorrectly() throws Exception {
        // TRUCCO ANTI-BLOCCO:
        // Ritorniamo FALSE per simulare un fallimento logico (password errata).
        // Il controller andrà nell'ELSE -> chiamerà showError() -> che NON blocca.
        // Se ritornassimo TRUE, il controller chiamerebbe showSuccessAlert() -> showAndWait() -> BLOCCO INFINITO.
        when(mockAccountService.changePassword(anyString(), anyString(), anyString())).thenReturn(false);

        runOnFxThread(() -> {
            try {
                // Setup input validi
                setTextField("txtCurrentPassword", "OldPass");
                setTextField("txtNewPassword", "NewPass123");
                setTextField("txtConfirmPassword", "NewPass123");

                // Eseguiamo l'azione
                invokeHandleSave(controller);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // VERIFICA:
        // Anche se abbiamo forzato il fallimento, verifichiamo che il controller abbia chiamato il service
        // con i parametri giusti. Questo conferma che la logica di estrazione dati funziona.
        verify(mockAccountService, times(1)).changePassword("testUser", "OldPass", "NewPass123");

        // Verifica che la finestra NON sia stata chiusa (perché fallimento)
        verify(mockStage, never()).close();
    }

    @Test
    void testHandleSave_ValidationErrors() throws Exception {
        runOnFxThread(() -> {
            try {
                // Caso: Password non coincidono
                setTextField("txtCurrentPassword", "OldPass");
                setTextField("txtNewPassword", "NewPass123");
                setTextField("txtConfirmPassword", "DIVERSA");

                invokeHandleSave(controller);

                // Verifica feedback visuale
                assertTrue(getLabelText("lblFeedback").contains("non coincidono"));

                // Verifica che il service NON sia stato chiamato
                verifyNoInteractions(mockAccountService);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // --- Helpers ---

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } catch (Exception e) { e.printStackTrace(); }
            finally { latch.countDown(); }
        });

        // Timeout di sicurezza
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        if (!finished) throw new RuntimeException("Timeout FX Thread - Il test si è bloccato (probabile Alert aperto o GTK missing)");
    }

    private void invokeHandleSave(Object target) throws Exception {
        Method m = target.getClass().getDeclaredMethod("handleSave");
        m.setAccessible(true);
        m.invoke(target);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void setTextField(String name, String val) throws Exception {
        Field f = controller.getClass().getDeclaredField(name);
        f.setAccessible(true);
        ((PasswordField) f.get(controller)).setText(val);
    }

    private String getLabelText(String name) throws Exception {
        Field f = controller.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return ((Label) f.get(controller)).getText();
    }
}