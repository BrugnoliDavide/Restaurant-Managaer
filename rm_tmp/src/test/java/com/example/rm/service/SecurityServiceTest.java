package com.example.rm.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class SecurityServiceTest {

    private static Connection h2Connection;
    private static MockedStatic<DatabaseService> mockedDatabaseService;

    @BeforeAll
    static void setupDB() throws Exception {
        h2Connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", ""
        );

        try (Statement st = h2Connection.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS USERS (" +
                            " username VARCHAR(50) PRIMARY KEY," +
                            " password VARCHAR(255) NOT NULL," +
                            " role VARCHAR(50) NOT NULL" +
                            ")"
            );
        }

        String rawPassword = "testPassword";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        try (PreparedStatement ps = h2Connection.prepareStatement(
                "INSERT INTO USERS (username, password, role) VALUES (?, ?, ?)"
        )) {
            ps.setString(1, "testUser");
            ps.setString(2, hash);
            ps.setString(3, "manager");
            ps.executeUpdate();
        }

        mockedDatabaseService = mockStatic(DatabaseService.class);
        mockedDatabaseService.when(DatabaseService::getConnection).thenReturn(h2Connection);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mockedDatabaseService != null) mockedDatabaseService.close();
        if (h2Connection != null && !h2Connection.isClosed()) h2Connection.close();
    }

    @Test
    void testAuthenticateSuccess() {
        String role = SecurityService.authenticate("testUser", "testPassword");
        assertEquals("manager", role);
    }

    @Test
    void testAuthenticateFailure_WrongPassword() {
        String role = SecurityService.authenticate("testUser", "wrongPassword");
        assertNull(role);
    }

    @Test
    void testAuthenticateFailure_UnknownUser() {
        String role = SecurityService.authenticate("unknownUser", "whatever");
        assertNull(role);
    }
}
