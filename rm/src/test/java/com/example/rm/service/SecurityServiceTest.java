package com.example.rm.service;

import com.example.rm.exception.AuthenticationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mindrot.jbcrypt.BCrypt;

import com.example.rm.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class SecurityServiceTest {

    private static Connection h2Connection;
    private static MockedStatic<DatabaseConnection> mockedDatabaseConnection;

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

        mockedDatabaseConnection = mockStatic(DatabaseConnection.class);

        mockedDatabaseConnection.when(DatabaseConnection::getConnection).thenAnswer(invocation ->
                DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "")
        );
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mockedDatabaseConnection != null) mockedDatabaseConnection.close();
        if (h2Connection != null && !h2Connection.isClosed()) h2Connection.close();
    }

    @Test
    void testAuthenticateFailure_WrongPassword() {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> SecurityService.authenticate("testUser", "wrongPassword")
        );
        assertEquals("Credenziali non valide", exception.getUserMessage());
    }

    @Test
    void testAuthenticateFailure_UnknownUser() {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> SecurityService.authenticate("unknownUser", "whatever")
        );
        assertEquals("Credenziali non valide", exception.getUserMessage());
    }

    @Test
    void testAuthenticateFailure_NullUsername() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SecurityService.authenticate(null, "password")
        );
        assertEquals("Username o password non validi", exception.getMessage());
    }

    @Test
    void testAuthenticateFailure_BlankPassword() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SecurityService.authenticate("testUser", "")
        );
        assertEquals("Username o password non validi", exception.getMessage());
    }
}