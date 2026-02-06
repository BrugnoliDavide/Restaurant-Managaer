package com.example.rm.dao;

import com.example.rm.preference.DemoModeManager;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.preference.PreferencesConstants;
import com.example.rm.preference.PreferencesSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseKitchenPreferencesDAOTest {

    private MockedStatic<DatabaseConnection> mockedDatabaseConnection;
    private MockedStatic<PreferencesSerializer> mockedPreferencesSerializer;
    private MockedStatic<DemoModeManager> mockedDemoModeManager;

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    private KitchenPreferences testPreferences;
    private String testUsername = "test_kitchen";
    private String serializedPreferences = "serialized_preferences_data";

    @BeforeEach
    void setUp() throws SQLException {
        // Mock delle classi statiche
        mockedDatabaseConnection = Mockito.mockStatic(DatabaseConnection.class);
        mockedPreferencesSerializer = Mockito.mockStatic(PreferencesSerializer.class);
        mockedDemoModeManager = Mockito.mockStatic(DemoModeManager.class);

        // Mock degli oggetti SQL
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // Configura i mock statici
        mockedDatabaseConnection.when(DatabaseConnection::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Crea oggetti di test
        testPreferences = new KitchenPreferences(
                testUsername,
                PreferencesConstants.DEFAULT_SPLIT_ORDERS,
                new HashSet<>(),
                PreferencesConstants.DEFAULT_INCLUDE_OTHER
        );

        // Configura i mock per la serializzazione
        mockedPreferencesSerializer.when(() ->
                        PreferencesSerializer.serialize(any(KitchenPreferences.class)))
                .thenReturn(serializedPreferences);

        mockedPreferencesSerializer.when(() ->
                        PreferencesSerializer.deserialize(eq(serializedPreferences), eq(testUsername)))
                .thenReturn(testPreferences);

        mockedPreferencesSerializer.when(() ->
                        PreferencesSerializer.deserialize(isNull(), eq(testUsername)))
                .thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        // Chiudi tutti i mock statici
        if (mockedDatabaseConnection != null) {
            mockedDatabaseConnection.close();
        }
        if (mockedPreferencesSerializer != null) {
            mockedPreferencesSerializer.close();
        }
        if (mockedDemoModeManager != null) {
            mockedDemoModeManager.close();
        }
    }

    @Test
    void testGetKitchenPreferences_Success() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("kitchen_preferences")).thenReturn(serializedPreferences);

        // Act
        KitchenPreferences result = DatabaseKitchenPreferencesDAO.getKitchenPreferences(testUsername);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        verify(mockPreparedStatement).setString(1, testUsername);
        verify(mockPreparedStatement).executeQuery();
    }

    @Test
    void testGetKitchenPreferences_NoResult() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Act
        KitchenPreferences result = DatabaseKitchenPreferencesDAO.getKitchenPreferences(testUsername);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(PreferencesConstants.DEFAULT_SPLIT_ORDERS, result.isSplitMixedCategoryOrders());
        assertEquals(PreferencesConstants.DEFAULT_INCLUDE_OTHER, result.isIncludeOtherCategories());
        verify(mockPreparedStatement).setString(1, testUsername);
    }

    @Test
    void testGetKitchenPreferences_SQLException() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        // Act
        KitchenPreferences result = DatabaseKitchenPreferencesDAO.getKitchenPreferences(testUsername);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        verify(mockPreparedStatement).setString(1, testUsername);
    }

    @Test
    void testSaveKitchenPreferences_Success() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.saveKitchenPreferences(testPreferences);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setString(1, serializedPreferences);
        verify(mockPreparedStatement).setString(2, testUsername);
        verify(mockPreparedStatement).executeUpdate();
    }


    @Test
    void testSaveKitchenPreferences_ZeroRowsAffected() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.saveKitchenPreferences(testPreferences);

        // Assert
        assertFalse(result);
        verify(mockPreparedStatement).setString(1, serializedPreferences);
        verify(mockPreparedStatement).setString(2, testUsername);
    }

    @Test
    void testSaveKitchenPreferences_SQLException() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.saveKitchenPreferences(testPreferences);

        // Assert
        assertFalse(result);
        verify(mockPreparedStatement).setString(1, serializedPreferences);
        verify(mockPreparedStatement).setString(2, testUsername);
    }

    @Test
    void testSaveKitchenPreferences_NullSerialization() throws SQLException {
        // Arrange
        mockedPreferencesSerializer.when(() ->
                        PreferencesSerializer.serialize(any(KitchenPreferences.class)))
                .thenReturn(null);

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.saveKitchenPreferences(testPreferences);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setNull(1, Types.VARCHAR);
        verify(mockPreparedStatement).setString(2, testUsername);
    }

    @Test
    void testDeleteKitchenPreferences_Success() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.deleteKitchenPreferences(testUsername);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setString(1, testUsername);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testDeleteKitchenPreferences_ZeroRowsAffected() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.deleteKitchenPreferences(testUsername);

        // Assert
        assertFalse(result);
        verify(mockPreparedStatement).setString(1, testUsername);
    }

    @Test
    void testDeleteKitchenPreferences_SQLException() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.deleteKitchenPreferences(testUsername);

        // Assert
        assertFalse(result);
        verify(mockPreparedStatement).setString(1, testUsername);
    }

    @Test
    void testLoadByUsername() throws SQLException {
        // Arrange
        DatabaseKitchenPreferencesDAO dao = new DatabaseKitchenPreferencesDAO();
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("kitchen_preferences")).thenReturn(serializedPreferences);

        // Act
        KitchenPreferences result = dao.loadByUsername(testUsername);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        verify(mockPreparedStatement).setString(1, testUsername);
    }

    @Test
    void testSave_NonDemoMode() throws SQLException {
        // Arrange
        DatabaseKitchenPreferencesDAO dao = new DatabaseKitchenPreferencesDAO();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        mockedDemoModeManager.when(DemoModeManager::isDemoMode).thenReturn(false);

        // Act
        boolean result = dao.save(testPreferences);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).executeUpdate();
        mockedDemoModeManager.verify(DemoModeManager::isDemoMode);
    }

    @Test
    void testSaveKitchenPreferences_NullUsername() throws SQLException {
        // Arrange
        KitchenPreferences nullUserPrefs = new KitchenPreferences(
                null,
                true,
                new HashSet<>(),
                false
        );

        // Act
        boolean result = DatabaseKitchenPreferencesDAO.saveKitchenPreferences(nullUserPrefs);

        // Assert
        assertFalse(result);
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    void testSaveKitchenPreferences_NullPreferences() throws SQLException {
        // Act
        boolean result = DatabaseKitchenPreferencesDAO.saveKitchenPreferences(null);

        // Assert
        assertFalse(result);
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    void testSave_DemoMode() throws SQLException {
        // Arrange
        DatabaseKitchenPreferencesDAO dao = new DatabaseKitchenPreferencesDAO();
        mockedDemoModeManager.when(DemoModeManager::isDemoMode).thenReturn(true);

        // Configura esplicitamente cosa succede se executeUpdate viene chiamato
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = dao.save(testPreferences);

        // Assert
        assertTrue(result);
        mockedDemoModeManager.verify(DemoModeManager::isDemoMode);
        // Verifica che executeUpdate non sia stato chiamato
        verify(mockPreparedStatement, never()).executeUpdate();
    }

    @Test
    void testDeleteByUsername() throws SQLException {
        // Arrange
        DatabaseKitchenPreferencesDAO dao = new DatabaseKitchenPreferencesDAO();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = dao.deleteByUsername(testUsername);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setString(1, testUsername);
        verify(mockPreparedStatement).executeUpdate();
    }
    @Test
    void testGetKitchenPreferences_DeserializationError() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("kitchen_preferences")).thenReturn("invalid_data");

        mockedPreferencesSerializer.when(() ->
                        PreferencesSerializer.deserialize("invalid_data", testUsername))
                .thenReturn(null);

        // Act & Assert
        // Verifica solo che il metodo non lanci eccezioni
        assertDoesNotThrow(() -> {
            KitchenPreferences result = DatabaseKitchenPreferencesDAO.getKitchenPreferences(testUsername);
            // Non fare asserzioni sul risultato
        });

        // Verifica che il metodo sia stato chiamato correttamente
        verify(mockPreparedStatement).setString(1, testUsername);
    }
}