package com.example.rm.view.component;

import com.example.rm.app.UserSession;
import com.example.rm.controller.UserAccountUseCase;
import com.example.rm.model.User;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangePasswordDialogControllerTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        new JFXPanel(); // init JavaFX toolkit
    }

    private ChangePasswordDialogController controller;
    private UserAccountUseCase accountServiceMock;
    private MockedStatic<UserSession> userSessionMockedStatic;



    @BeforeEach
    void setUp() throws Exception {
        controller = new ChangePasswordDialogController();
        accountServiceMock = mock(UserAccountUseCase.class);

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Inject FXML fields (creati su FX thread)
                setField(controller, "lblTitle", new Label());
                setField(controller, "lblSubtitle", new Label());
                setField(controller, "lblUsernameLabel", new Label());
                setField(controller, "lblUsername", new Label());
                setField(controller, "lblRequirements", new Label());
                setField(controller, "lblFeedback", new Label());
                setField(controller, "txtCurrentPassword", new PasswordField());
                setField(controller, "txtNewPassword", new PasswordField());
                setField(controller, "txtConfirmPassword", new PasswordField());
                setField(controller, "btnCancel", new Button());
                setField(controller, "btnSave", new Button());

                // Stage: DEVE essere creato sul JavaFX Application Thread
                setField(controller, "stage", new Stage());

                // Inject service mock
                setField(controller, "accountService", accountServiceMock);

                // Evita mockStatic(UserSession) e initialize(): setta direttamente lo username usato da handleSave()
                setField(controller, "username", "testUser");
                ((Label) getField(controller, "lblUsername", Label.class)).setText("testUser");
                ((Label) getField(controller, "lblFeedback", Label.class)).setVisible(false);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout inizializzazione JavaFX");
    }


    @AfterEach
    void tearDown() {
        if (userSessionMockedStatic != null) {
            userSessionMockedStatic.close();
        }
    }

    @Test
    void handleSave_WhenFieldsEmpty_ShowsErrorAndDoesNotCallService() throws Exception {
        PasswordField cur = getField(controller, "txtCurrentPassword", PasswordField.class);
        PasswordField nw = getField(controller, "txtNewPassword", PasswordField.class);
        PasswordField conf = getField(controller, "txtConfirmPassword", PasswordField.class);

        cur.setText("");
        nw.setText("");
        conf.setText("");

        invokeNoArgs(controller, "handleSave");

        Label feedback = getField(controller, "lblFeedback", Label.class);
        assertTrue(feedback.isVisible());
        assertTrue(feedback.getText().contains("Tutti i campi sono obbligatori"));

        verifyNoInteractions(accountServiceMock);
    }

    @Test
    void handleSave_WhenPasswordsDoNotMatch_ShowsErrorAndDoesNotCallService() throws Exception {
        PasswordField cur = getField(controller, "txtCurrentPassword", PasswordField.class);
        PasswordField nw = getField(controller, "txtNewPassword", PasswordField.class);
        PasswordField conf = getField(controller, "txtConfirmPassword", PasswordField.class);

        cur.setText("oldPass");
        nw.setText("NewPass123");
        conf.setText("OtherPass");

        invokeNoArgs(controller, "handleSave");

        Label feedback = getField(controller, "lblFeedback", Label.class);
        assertTrue(feedback.isVisible());
        assertTrue(feedback.getText().contains("non coincidono"));

        verifyNoInteractions(accountServiceMock);
    }

    @Test
    void handleSave_WhenValid_CallsServiceWithExpectedArgs() throws Exception {
        PasswordField cur = getField(controller, "txtCurrentPassword", PasswordField.class);
        PasswordField nw = getField(controller, "txtNewPassword", PasswordField.class);
        PasswordField conf = getField(controller, "txtConfirmPassword", PasswordField.class);

        cur.setText("oldPass");
        nw.setText("NewPass123");
        conf.setText("NewPass123");

        when(accountServiceMock.changePassword("testUser", "oldPass", "NewPass123"))
                .thenReturn(false); // evita showSuccessAlert() che apre una finestra

        invokeNoArgs(controller, "handleSave");

        verify(accountServiceMock, times(1))
                .changePassword("testUser", "oldPass", "NewPass123");
    }

    // ---------- reflection helpers ----------

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    private static Object invokeNoArgs(Object target, String methodName) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(target);
    }
}
