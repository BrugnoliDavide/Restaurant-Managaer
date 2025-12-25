package com.example.rm.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTest {

    private MockedStatic<DatabaseService> mockedDatabaseService;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        // Inizializziamo i mock per JDBC
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // Creiamo il "finto" DatabaseService statico
        mockedDatabaseService = mockStatic(DatabaseService.class);
    }

    @AfterEach
    void tearDown() {
        // Fondamentale: chiudere il mock statico per evitare conflitti tra test
        mockedDatabaseService.close();
    }

    @Test
    void testAuthenticateSuccess() throws SQLException {
        // GIVEN
        String username = "testUser";
        String password = "testPassword";
        String role = "manager";
        // Creiamo un hash valido per la password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Comportamento del DatabaseService
        mockedDatabaseService.when(DatabaseService::getConnection).thenReturn(mockConnection);

        // Comportamento degli oggetti JDBC
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Simuliamo il risultato del database
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("password")).thenReturn(hashedPassword);
        when(mockResultSet.getString("role")).thenReturn(role);

        // WHEN
        String resultRole = SecurityService.authenticate(username, password);

        // THEN
        assertEquals(role, resultRole, "Dovrebbe restituire il ruolo 'manager'");
    }

    @Test
    void testAuthenticateFailure() throws SQLException {
        // GIVEN
        mockedDatabaseService.when(DatabaseService::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Simuliamo utente non trovato
        when(mockResultSet.next()).thenReturn(false);

        // WHEN
        String resultRole = SecurityService.authenticate("wrongUser", "anyPassword");

        // THEN
        assertNull(resultRole, "Dovrebbe restituire null se l'utente non esiste");
    }
}