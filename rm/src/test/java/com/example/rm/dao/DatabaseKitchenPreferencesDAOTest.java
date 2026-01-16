package com.example.rm.dao;

import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseKitchenPreferencesDAOTest {

    private MockedStatic<DatabaseService> mockedDbService;
    private KitchenPreferencesDAO dao;

    @BeforeEach
    void setUp() {
        mockedDbService = mockStatic(DatabaseService.class);
        dao = new DatabaseKitchenPreferencesDAO();
    }

    @AfterEach
    void tearDown() {
        mockedDbService.close();
    }

    @Test
    void loadByUsername_CallsDatabaseServiceGetKitchenPreferences() {
        String username = "chef1";
        KitchenPreferences expectedPrefs = new KitchenPreferences();
        expectedPrefs.setSplitMixedCategoryOrders(true);
        expectedPrefs.setSelectedCategories(Set.of("Primi", "Secondi"));

        mockedDbService.when(() -> DatabaseService.getKitchenPreferences(username))
                .thenReturn(expectedPrefs);

        KitchenPreferences result = dao.loadByUsername(username);

        assertEquals(expectedPrefs, result);
        mockedDbService.verify(() -> DatabaseService.getKitchenPreferences(username), times(1));
    }

    @Test
    void save_ReturnsTrue_WhenDatabaseServiceSucceeds() {
        KitchenPreferences prefs = new KitchenPreferences();
        mockedDbService.when(() -> DatabaseService.saveKitchenPreferences(prefs))
                .thenReturn(true);

        boolean result = dao.save(prefs);

        assertTrue(result);
        mockedDbService.verify(() -> DatabaseService.saveKitchenPreferences(prefs), times(1));
    }

    @Test
    void save_ReturnsFalse_WhenDatabaseServiceFails() {
        KitchenPreferences prefs = new KitchenPreferences();
        mockedDbService.when(() -> DatabaseService.saveKitchenPreferences(prefs))
                .thenReturn(false);

        boolean result = dao.save(prefs);

        assertFalse(result);
        mockedDbService.verify(() -> DatabaseService.saveKitchenPreferences(prefs), times(1));
    }

    @Test
    void deleteByUsername_ReturnsTrue_WhenDatabaseServiceSucceeds() {
        String username = "chef1";
        mockedDbService.when(() -> DatabaseService.deleteKitchenPreferences(username))
                .thenReturn(true);

        boolean result = dao.deleteByUsername(username);

        assertTrue(result);
        mockedDbService.verify(() -> DatabaseService.deleteKitchenPreferences(username), times(1));
    }
}
