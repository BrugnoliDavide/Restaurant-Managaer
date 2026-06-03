package rm.dao;

import rm.service.ConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import rm.bean.ProductBean;

class DatabaseProductDAOMockStaticTest {

    private MockedStatic<ConnectionManager> mockedConnectionManager;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private DatabaseProductDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        mockedConnectionManager = Mockito.mockStatic(ConnectionManager.class);
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockedConnectionManager.when(ConnectionManager::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        dao = new DatabaseProductDAO();
    }

    @AfterEach
    void tearDown() {
        if (mockedConnectionManager != null) {
            mockedConnectionManager.close();
        }
    }

    // --- INSERT (tramite save con id <= 0) ---

    @Test
    void testSave_NewProduct_Success() throws SQLException {
        ProductBean product = createProduct(0, "Pizza Margherita",
                "Piatto principale", BigDecimal.valueOf(12.5), BigDecimal.valueOf(4.5), "Glutine");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(dao.save(product));
        verify(mockPreparedStatement).setString(1, "Pizza Margherita");
        verify(mockPreparedStatement).setString(2, "Piatto principale");
        verify(mockPreparedStatement).setBigDecimal(3, new BigDecimal("12.5"));
        verify(mockPreparedStatement).setBigDecimal(4, new BigDecimal("4.5"));
        verify(mockPreparedStatement).setString(5, "Glutine");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testSave_NewProduct_Failure() throws SQLException {
        ProductBean product = new ProductBean();
        product.setNome("Pizza");

        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(dao.save(product));
    }

    @Test
    void testSave_NewProduct_SQLException() throws SQLException {
        ProductBean product = new ProductBean();
        product.setNome("Pizza");

        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        assertFalse(dao.save(product));
    }

    // --- UPDATE (tramite save con id > 0) ---

    @Test
    void testSave_ExistingProduct_Success() throws SQLException {
        ProductBean product = createProduct(1, "Pizza Margherita",
                "Piatto principale", BigDecimal.valueOf(12.5),BigDecimal.valueOf( 4.5), "Glutine");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(dao.save(product));
        verify(mockPreparedStatement).setString(1, "Pizza Margherita");
        verify(mockPreparedStatement).setString(2, "Piatto principale");
        verify(mockPreparedStatement).setBigDecimal(3, new BigDecimal("12.5"));
        verify(mockPreparedStatement).setBigDecimal(4, new BigDecimal("4.5"));
        verify(mockPreparedStatement).setString(5, "Glutine");
        verify(mockPreparedStatement).setInt(6, 1);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testSave_ExistingProduct_VerifyId() throws SQLException {
        ProductBean product = new ProductBean();
        product.setId(1);
        product.setNome("Pizza");

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(dao.save(product));
        verify(mockPreparedStatement).setInt(6, 1);
    }

    // --- DELETE (tramite interfaccia) ---

    @Test
    void testDelete_Success() throws SQLException {
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(dao.delete(1L));
        verify(mockPreparedStatement).setLong(1, 1L);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testDelete_NoRowsAffected() throws SQLException {
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(dao.delete(1L));
    }

    @Test
    void testDelete_SQLException() throws SQLException {
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        assertFalse(dao.delete(1L));
    }

    // --- SAVE null ---

    @Test
    void testSave_NullProduct() {
        assertFalse(dao.save(null));
    }

    // --- DELETE invalidi ---

    @Test
    void testDelete_NullId() {
        assertFalse(dao.delete(null));
    }

    @Test
    void testDelete_InvalidId() {
        assertFalse(dao.delete(0L));
    }

    // --- Helper ---

    private ProductBean createProduct(int id, String nome, String tipologia,
                                      BigDecimal prezzo, BigDecimal costo, String allergeni) {
        ProductBean p = new ProductBean();
        p.setId(id);
        p.setNome(nome);
        p.setTipologia(tipologia);
        p.setPrezzoVendita(prezzo);
        p.setCostoRealizzazione(costo);
        p.setAllergeni(allergeni);
        return p;
    }
}