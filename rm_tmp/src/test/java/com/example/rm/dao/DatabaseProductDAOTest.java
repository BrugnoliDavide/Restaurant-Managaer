package com.example.rm.dao;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseProductDAOTest {

    private MockedStatic<DatabaseService> mockedDb;
    private DatabaseProductDAO dao;

    @BeforeEach
    void setUp() {
        mockedDb = mockStatic(DatabaseService.class);
        dao = new DatabaseProductDAO();
    }

    @AfterEach
    void tearDown() {
        mockedDb.close();
    }

    @Test
    void save_WhenProductNull_ReturnsFalseAndDoesNotCallDb() {
        boolean result = dao.save(null);

        assertFalse(result);
        mockedDb.verifyNoInteractions();
    }

    @Test
    void save_WhenNewProduct_IdIsZero_CallsAddProduct() {
        MenuProduct p = new MenuProduct();
        p.setId(0); // MenuProduct default id = 0, ma lo settiamo esplicitamente[ file:61 ]

        mockedDb.when(() -> DatabaseService.addProduct(p)).thenReturn(true);

        boolean result = dao.save(p);

        assertTrue(result);
        mockedDb.verify(() -> DatabaseService.addProduct(p), times(1));
        mockedDb.verify(() -> DatabaseService.updateProduct(any(MenuProduct.class)), never());
    }

    @Test
    void save_WhenExistingProduct_IdGreaterThanZero_CallsUpdateProduct() {
        MenuProduct p = new MenuProduct();
        p.setId(10);

        mockedDb.when(() -> DatabaseService.updateProduct(p)).thenReturn(true);

        boolean result = dao.save(p);

        assertTrue(result);
        mockedDb.verify(() -> DatabaseService.updateProduct(p), times(1));
        mockedDb.verify(() -> DatabaseService.addProduct(any(MenuProduct.class)), never());
    }

    @Test
    void delete_WhenIdNull_ReturnsFalseAndDoesNotCallDb() {
        boolean result = dao.delete(null);

        assertFalse(result);
        mockedDb.verifyNoInteractions();
    }

    @Test
    void delete_WhenIdNonPositive_ReturnsFalseAndDoesNotCallDb() {
        assertFalse(dao.delete(0L));
        assertFalse(dao.delete(-5L));

        mockedDb.verifyNoInteractions();
    }

    @Test
    void delete_WhenValidId_CallsDatabaseDeleteProductWithInt() {
        mockedDb.when(() -> DatabaseService.deleteProduct(7)).thenReturn(true);

        boolean result = dao.delete(7L);

        assertTrue(result);
        mockedDb.verify(() -> DatabaseService.deleteProduct(7), times(1));
    }
}
