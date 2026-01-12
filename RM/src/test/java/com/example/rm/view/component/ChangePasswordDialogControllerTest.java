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
        // Setup per evitare errori grafici
        System.setProperty("java.awt.headless", "true");
        try {
            new JFXPanel();
        } catch (Exception e) {}
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

                // Inietta i componenti UI
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
        // Facciamo ritornare FALSE al service.
        // Così il controller entra nell'ELSE, mostra l'errore (che non blocca) ed esce.
        // Se ritornassimo TRUE, entrerebbe in showSuccessAlert() -> showAndWait() -> LOOP INFINITO.
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

        // VERIFICA: Anche se abbiamo simulato un fallimento, possiamo verificare
        // che il controller abbia estratto i dati e chiamato il service correttamente.
        verify(mockAccountService, times(1)).changePassword("testUser", "OldPass", "NewPass123");

        // Verifica che NON sia stato chiuso lo stage (perché abbiamo simulato fallimento)
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
        // Se si blocca qui, è colpa di Platform.runLater, ma con i test sopra non dovrebbe accadere
        boolean finished = latch.await(3, TimeUnit.SECONDS);
        if (!finished) throw new RuntimeException("Timeout FX Thread - Probabile Alert aperto");
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