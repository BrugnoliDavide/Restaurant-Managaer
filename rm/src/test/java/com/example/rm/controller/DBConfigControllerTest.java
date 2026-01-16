package com.example.rm.controller;

import com.example.rm.service.DBConfigStore;
import com.example.rm.service.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DBConfigControllerTest {

    private MockedStatic<DatabaseService> mockedDbService;
    private MockedStatic<DBConfigStore> mockedConfigStore;
    private DBConfigController controller;

    @BeforeEach
    void setUp() {
        mockedDbService = mockStatic(DatabaseService.class);
        mockedConfigStore = mockStatic(DBConfigStore.class);
        controller = new DBConfigController();
    }

    @AfterEach
    void tearDown() {
        mockedDbService.close();
        mockedConfigStore.close();
    }

    @Test
    void loadConfig_ReturnsCurrentConfiguration() {
        mockedDbService.when(DatabaseService::getDBHost).thenReturn("localhost");
        mockedDbService.when(DatabaseService::getDBPort).thenReturn("5432");
        mockedDbService.when(DatabaseService::getDBName).thenReturn("testdb");
        mockedDbService.when(DatabaseService::getDBUser).thenReturn("user");
        mockedDbService.when(DatabaseService::hasPassword).thenReturn(true);

        DBConfigUseCase.DBConfig config = controller.loadConfig();

        assertEquals("localhost", config.host);
        assertEquals("5432", config.port);
        assertEquals("testdb", config.dbName);
        assertEquals("user", config.username);
        assertTrue(config.hasPassword);
    }

    @Test
    void saveConfig_ReturnsFalse_WhenHostEmpty() {
        boolean result = controller.saveConfig("", "5432", "db", "user", "pass");

        assertFalse(result);
        mockedConfigStore.verifyNoInteractions();
        mockedDbService.verify(
                () -> DatabaseService.setConnectionConfig(any(), any(), any(), any(), any()),
                never()
        );
    }

    @Test
    void saveConfig_SavesSuccessfully_WithValidInput() {
         boolean result = controller.saveConfig("localhost", "5432", "testdb", "user", "pass123");

        assertTrue(result);

        mockedConfigStore.verify(() ->
                        DBConfigStore.save("localhost", "5432", "testdb", "user", "pass123"),
                times(1)
        );
        mockedDbService.verify(() ->
                        DatabaseService.setConnectionConfig("localhost", "5432", "testdb", "user", "pass123"),
                times(1)
        );
    }

    @Test
    void saveConfig_UsesExistingPassword_WhenNewPasswordEmpty() {
        mockedConfigStore.when(DBConfigStore::getPassword).thenReturn("oldpass");

        boolean result = controller.saveConfig("localhost", "5432", "db", "user", "");

        assertTrue(result);

        mockedConfigStore.verify(DBConfigStore::getPassword, times(1));
        mockedConfigStore.verify(() ->
                        DBConfigStore.save("localhost", "5432", "db", "user", "oldpass"),
                times(1)
        );
        mockedDbService.verify(() ->
                        DatabaseService.setConnectionConfig("localhost", "5432", "db", "user", "oldpass"),
                times(1)
        );
    }
}
