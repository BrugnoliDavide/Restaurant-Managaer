package com.example.rm.controller;

import com.example.rm.dao.KitchenPreferencesDAO;
import com.example.rm.preference.KitchenPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitchenPreferencesControllerTest {

    private KitchenPreferencesDAO daoMock;
    private KitchenPreferencesController controller;

    @BeforeEach
    void setUp() {
        daoMock = mock(KitchenPreferencesDAO.class);
        controller = new KitchenPreferencesController(daoMock);
    }

    @Test
    void load_DelegatesToDAO() {
        String username = "chef1";
        KitchenPreferences expected = new KitchenPreferences();
        expected.setSplitMixedCategoryOrders(true);
        expected.setSelectedCategories(Set.of("Primi"));

        when(daoMock.loadByUsername(username)).thenReturn(expected);

        KitchenPreferences result = controller.load(username);

        assertEquals(expected, result);
        verify(daoMock).loadByUsername(username);
        verifyNoMoreInteractions(daoMock);
    }

    @Test
    void save_DelegatesToDAO_ReturnsTrue() {
        KitchenPreferences prefs = new KitchenPreferences();
        when(daoMock.save(prefs)).thenReturn(true);

        boolean result = controller.save(prefs);

        assertTrue(result);
        verify(daoMock).save(prefs);
        verifyNoMoreInteractions(daoMock);
    }

    @Test
    void reset_DelegatesToDAO() {
        String username = "chef1";
        when(daoMock.deleteByUsername(username)).thenReturn(true);

        boolean result = controller.reset(username);

        assertTrue(result);
        verify(daoMock).deleteByUsername(username);
        verifyNoMoreInteractions(daoMock);
    }
}
