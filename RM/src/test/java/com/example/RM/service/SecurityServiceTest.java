package com.example.RM.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityServiceTest {

    @BeforeAll
    static void setup() {
        // Configura DatabaseService per usare un database locale di test o H2
        DatabaseService.setConnectionConfig("localhost", "5432", "restaurant_db", "admin", "password123");
    }

    @Test
    @DisplayName("Autenticazione fallisce con password errata")
    void testAuthenticateFailure() {
        // Arrange: registriamo un utente (o diamo per scontato che esista)
        String user = "testUser";
        String pass = "correctPass";
        SecurityService.registerUser(user, pass, "manager");

        // Act
        String role = SecurityService.authenticate(user, "wrongPass");

        // Assert
        assertNull(role, "Il ruolo dovrebbe essere null per password errata");
    }

    @Test
    @DisplayName("Autenticazione ha successo con credenziali corrette")
    void testAuthenticateSuccess() {
        // Act
        String role = SecurityService.authenticate("testUser", "correctPass");

        // Assert
        assertEquals("manager", role);
    }
}