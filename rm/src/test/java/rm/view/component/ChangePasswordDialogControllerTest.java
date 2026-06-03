package rm.view.component;

import rm.controller.UserAccountUseCase;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChangePasswordDialogControllerTest {

    @BeforeAll
    static void initToolkit() {
        // Rimuoviamo tutte le proprietà "Monocle" che causano l'errore
        // Manteniamo solo queste per stabilità:
        System.setProperty("java.awt.headless", "true"); // Per Swing/AWT
        System.setProperty("prism.order", "sw");         // Forza rendering software (no GPU)
        System.setProperty("prism.text", "t2k");         // Rendering testo software

        try {
            new JFXPanel();
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile inizializzare JavaFX Toolkit", e);
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
                // Iniezione dipendenze
                setField(controller, "accountService", mockAccountService);
                setField(controller, "stage", mockStage);
                setField(controller, "username", "testUser");

                // Iniezione componenti UI
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
        // SETUP: Il service ritorna FALSE.
        // Questo evita che il controller chiami showSuccessAlert() -> showAndWait().
        // Il test verificherà comunque che i dati siano stati passati correttamente.
        when(mockAccountService.changePassword(anyString(), anyString(), anyString())).thenReturn(false);

        runOnFxThread(() -> {
            try {
                setTextField("txtCurrentPassword", "OldPass");
                setTextField("txtNewPassword", "NewPass123");
                setTextField("txtConfirmPassword", "NewPass123");

                invokeHandleSave(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // VERIFICA: Confermiamo che il service è stato chiamato con i valori giusti
        verify(mockAccountService, times(1)).changePassword("testUser", "OldPass", "NewPass123");

        // VERIFICA: Confermiamo che NON si è bloccato e non ha chiuso la finestra (causa errore simulato)
        verify(mockStage, never()).close();
    }

    // ... (Mantieni gli altri metodi helper uguali a prima: runOnFxThread, invokeHandleSave, setField, ecc.)

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } catch (Exception e) { e.printStackTrace(); }
            finally { latch.countDown(); }
        });
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        if (!finished) throw new RuntimeException("Timeout FX Thread");
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
}