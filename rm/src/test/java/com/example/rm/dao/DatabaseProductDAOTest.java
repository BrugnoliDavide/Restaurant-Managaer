package com.example.rm.dao;

import com.example.rm.model.MenuProduct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseProductDAOMockStaticTest {

    private MockedStatic<DatabaseConnection> mockedDatabaseConnection;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;

    @BeforeEach
    void setUp() throws SQLException {
        // Crea mock per DatabaseConnection
        mockedDatabaseConnection = Mockito.mockStatic(DatabaseConnection.class);

        // Crea mock per Connection e PreparedStatement
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);

        // Configura il mock di DatabaseConnection.getConnection()
        mockedDatabaseConnection.when(DatabaseConnection::getConnection).thenReturn(mockConnection);

        // Configura il mock della Connection
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        // Chiudi il mock statico
        if (mockedDatabaseConnection != null) {
            mockedDatabaseConnection.close();
        }
    }

    @Test
    void testAddProduct_Success() throws SQLException {
        // Arrange
        MenuProduct product = new MenuProduct();
        product.setNome("Pizza Margherita");
        product.setTipologia("Piatto principale");
        product.setPrezzoVendita(12.5);
        product.setCostoRealizzazione(4.5);
        product.setAllergeni("Glutine");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = DatabaseProductDAO.addProduct(product);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "Pizza Margherita");
        verify(mockPreparedStatement).setString(2, "Piatto principale");
        verify(mockPreparedStatement).setDouble(3, 12.5);
        verify(mockPreparedStatement).setDouble(4, 4.5);
        verify(mockPreparedStatement).setString(5, "Glutine");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testAddProduct_Failure() throws SQLException {
        // Arrange
        MenuProduct product = new MenuProduct();
        product.setNome("Pizza");

        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = DatabaseProductDAO.addProduct(product);

        // Assert
        assertFalse(result);
    }

    @Test
    void testAddProduct_SQLException() throws SQLException {
        // Arrange
        MenuProduct product = new MenuProduct();
        product.setNome("Pizza");

        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act
        boolean result = DatabaseProductDAO.addProduct(product);

        // Assert
        assertFalse(result);
    }

    @Test
    void testUpdateProduct_Success() throws SQLException {
        // Arrange
        MenuProduct product = new MenuProduct();
        product.setId(1);
        product.setNome("Pizza Margherita");
        product.setTipologia("Piatto principale");
        product.setPrezzoVendita(12.5);
        product.setCostoRealizzazione(4.5);
        product.setAllergeni("Glutine");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = DatabaseProductDAO.updateProduct(product);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "Pizza Margherita");
        verify(mockPreparedStatement).setString(2, "Piatto principale");
        verify(mockPreparedStatement).setDouble(3, 12.5);
        verify(mockPreparedStatement).setDouble(4, 4.5);
        verify(mockPreparedStatement).setString(5, "Glutine");
        verify(mockPreparedStatement).setInt(6, 1);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testDeleteProduct_Success() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = DatabaseProductDAO.deleteProduct(1);

        // Assert
        assertTrue(result);
        verify(mockPreparedStatement).setInt(1, 1);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testDeleteProduct_NoRowsAffected() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = DatabaseProductDAO.deleteProduct(1);

        // Assert
        assertFalse(result);
    }

    @Test
    void testDeleteProduct_SQLException() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act
        boolean result = DatabaseProductDAO.deleteProduct(1);

        // Assert
        assertFalse(result);
    }

    @Test
    void testSave_NewProduct() throws SQLException {
        // Arrange
        DatabaseProductDAO dao = new DatabaseProductDAO();
        MenuProduct product = new MenuProduct();
        product.setId(0);
        product.setNome("Pizza Margherita");
        product.setTipologia("Piatto principale");
        product.setPrezzoVendita(12.5);
        product.setCostoRealizzazione(4.5);
        product.setAllergeni("Glutine");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = dao.save(product);

        // Assert
        assertTrue(result);
        // CORREZIONE: Verifica il numero corretto di chiamate
        // 3 stringhe: nome, tipologia, allergeni
        verify(mockPreparedStatement, times(3)).setString(anyInt(), anyString());
        // 2 double: prezzo, costo
        verify(mockPreparedStatement, times(2)).setDouble(anyInt(), anyDouble());
        // Verifica i parametri specifici
        verify(mockPreparedStatement).setString(1, "Pizza Margherita");
        verify(mockPreparedStatement).setString(2, "Piatto principale");
        verify(mockPreparedStatement).setDouble(3, 12.5);
        verify(mockPreparedStatement).setDouble(4, 4.5);
        verify(mockPreparedStatement).setString(5, "Glutine");
    }
    @Test
    void testSave_ExistingProduct() throws SQLException {
        // Arrange
        DatabaseProductDAO dao = new DatabaseProductDAO();
        MenuProduct product = new MenuProduct();
        product.setId(1);
        product.setNome("Pizza");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = dao.save(product);

        // Assert
        assertTrue(result);
        // Verifica che sia stata chiamata updateProduct (incluso l'ID)
        verify(mockPreparedStatement).setInt(6, 1);
    }
}