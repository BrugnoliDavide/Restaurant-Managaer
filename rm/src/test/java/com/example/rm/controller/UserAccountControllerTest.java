package com.example.rm.controller;

import com.example.rm.service.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class UserAccountControllerTest {

    private MockedStatic<SecurityService> mockedSecurityService;
    private UserAccountController controller;

    @BeforeEach
    void setUp() {
        mockedSecurityService = mockStatic(SecurityService.class);
        controller = new UserAccountController();
    }

    @AfterEach
    void tearDown() {
        mockedSecurityService.close();
    }

    @Test
    void changePassword_ReturnsFalse_WhenNewPasswordTooShort() {
        String username = "user";
        String current = "oldPass";
        String newPass = "123"; // < 6

        boolean result = controller.changePassword(username, current, newPass);

        assertFalse(result, "Dovrebbe restituire false per password troppo corta");
        mockedSecurityService.verifyNoInteractions();
    }

    @Test
    void changePassword_DelegatesToSecurityService_OnValidInput() {
        String username = "user";
        String current = "oldPass";
        String newPass = "NewPass123!";

        mockedSecurityService.when(() ->
                SecurityService.changePassword(username, current, newPass)
        ).thenReturn(true);

        boolean result = controller.changePassword(username, current, newPass);

        assertTrue(result, "Dovrebbe restituire il valore del SecurityService");
        mockedSecurityService.verify(() ->
                        SecurityService.changePassword(username, current, newPass),
                times(1)
        );
    }

    @Test
    void changePassword_ReturnsFalse_WhenSecurityServiceFails() {
        String username = "user";
        String current = "oldPass";
        String newPass = "NewPass123!";

        mockedSecurityService.when(() ->
                SecurityService.changePassword(username, current, newPass)
        ).thenReturn(false);

        boolean result = controller.changePassword(username, current, newPass);

        assertFalse(result, "Dovrebbe propagare il fallimento del SecurityService");
    }
}
