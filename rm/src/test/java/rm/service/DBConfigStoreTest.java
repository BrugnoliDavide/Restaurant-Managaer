package rm.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.prefs.Preferences;

/**
 * Suite di test per verificare il corretto funzionamento di DBConfigStore
 * con particolare attenzione alla cifratura AES/GCM
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DBConfigStoreTest {

    private static final String TEST_HOST = "localhost";
    private static final String TEST_PORT = "5432";
    private static final String TEST_DB = "test_database";
    private static final String TEST_USER = "test_user";
    private static final String TEST_PASSWORD = "MySecurePassword123!@#";

    @BeforeEach
    void setUp() {
        DBConfigStore.clearAll();
    }


    @AfterEach
    void tearDown() {
        DBConfigStore.clearAll();
    }

    @Test
    @Order(1)
    @DisplayName("Test salvataggio configurazione completa")
    void testSaveCompleteConfiguration() {
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, TEST_PASSWORD);

        assertEquals(TEST_HOST, DBConfigStore.getHost(), "Host non salvato correttamente");
        assertEquals(TEST_PORT, DBConfigStore.getPort(), "Port non salvato correttamente");
        assertEquals(TEST_DB, DBConfigStore.getDbName(), "Database name non salvato correttamente");
        assertEquals(TEST_USER, DBConfigStore.getUser(), "User non salvato correttamente");
        assertEquals(TEST_PASSWORD, DBConfigStore.getPassword(), "Password non decifrata correttamente");
    }

    @Test
    @Order(2)
    @DisplayName("Test cifratura e decifratura password")
    void testPasswordEncryptionDecryption() {
        String[] testPasswords = {
                "simple",
                "Complex!Password123",
                "P@ssw0rd_With_Special_Ch@rs!",
                "密碼測試", // Password con caratteri unicode "strani"
                "very_long_password_that_exceeds_normal_length_to_test_edge_cases_1234567890",
                ""
        };

        for (String password : testPasswords) {
            DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, password);
            String retrieved = DBConfigStore.getPassword();

            assertEquals(password, retrieved,
                    String.format("Password '%s' non decifrata correttamente", password));

            DBConfigStore.clearAll();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test salvataggio senza password")
    void testSaveWithoutPassword() {
        // Act
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, null);

        // Assert
        assertEquals(TEST_HOST, DBConfigStore.getHost());
        assertEquals(TEST_PORT, DBConfigStore.getPort());
        assertEquals(TEST_DB, DBConfigStore.getDbName());
        assertEquals(TEST_USER, DBConfigStore.getUser());
        assertEquals("", DBConfigStore.getPassword(), "Password dovrebbe essere vuota");
        assertFalse(DBConfigStore.hasValidPassword(), "Non dovrebbe avere password valida");
    }

    @Test
    @Order(4)
    @DisplayName("Test salvataggio password vuota o blank")
    void testSaveBlankPassword() {
        // Test con stringa vuota
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, "");
        assertEquals("", DBConfigStore.getPassword(), "Password vuota dovrebbe restare vuota");

        DBConfigStore.clearAll();

        // Test con solo spazi
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, "   ");
        assertEquals("", DBConfigStore.getPassword(), "Password blank non dovrebbe essere salvata");
    }

    @Test
    @Order(5)
    @DisplayName("Test recupero configurazione non esistente")
    void testGetNonExistentConfiguration() {
        // Assert - tutte le get dovrebbero restituire stringhe vuote
        assertEquals("", DBConfigStore.getHost());
        assertEquals("", DBConfigStore.getPort());
        assertEquals("", DBConfigStore.getDbName());
        assertEquals("", DBConfigStore.getUser());
        assertEquals("", DBConfigStore.getPassword());
        assertFalse(DBConfigStore.hasConfiguration());
        assertFalse(DBConfigStore.hasValidPassword());
    }

    @Test
    @Order(6)
    @DisplayName("Test clearAll rimuove tutta la configurazione")
    void testClearAll() {
        // Arrange
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, TEST_PASSWORD);

        // Verifica che sia salvato
        assertEquals(TEST_HOST, DBConfigStore.getHost());
        assertTrue(DBConfigStore.hasConfiguration());

        // Act
        DBConfigStore.clearAll();

        // Assert
        assertEquals("", DBConfigStore.getHost());
        assertEquals("", DBConfigStore.getPort());
        assertEquals("", DBConfigStore.getDbName());
        assertEquals("", DBConfigStore.getUser());
        assertEquals("", DBConfigStore.getPassword());
        assertFalse(DBConfigStore.hasConfiguration());
    }

    @Test
    @Order(7)
    @DisplayName("Test persistenza tra istanze")
    void testPersistenceAcrossInstances() {
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, TEST_PASSWORD);

        Preferences prefs = Preferences.userNodeForPackage(DBConfigStore.class);
        String savedHost = prefs.get("db.host", "");

        assertEquals(TEST_HOST, savedHost, "I dati non persistono correttamente");
        assertEquals(TEST_PASSWORD, DBConfigStore.getPassword(),
                "Password non persiste correttamente tra recuperi");
    }

    @Test
    @Order(8)
    @DisplayName("Test modifica password esistente")
    void testUpdatePassword() {
        String firstPassword = "FirstPassword123";
        String secondPassword = "SecondPassword456";

        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, firstPassword);
        assertEquals(firstPassword, DBConfigStore.getPassword());

        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, secondPassword);

        assertEquals(secondPassword, DBConfigStore.getPassword(),
                "Password non aggiornata correttamente");
        assertEquals(TEST_HOST, DBConfigStore.getHost(), "Host non dovrebbe cambiare");
    }

    @Test
    @Order(9)
    @DisplayName("Test hasConfiguration con configurazione parziale")
    void testHasConfigurationPartial() {
        // Test con solo alcuni campi
        DBConfigStore.save(TEST_HOST, "", "", "", "");
        assertFalse(DBConfigStore.hasConfiguration(),
                "hasConfiguration dovrebbe essere false con dati incompleti");

        DBConfigStore.clearAll();

        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, "");
        assertTrue(DBConfigStore.hasConfiguration(),
                "hasConfiguration dovrebbe essere true anche senza password");
    }

    @Test
    @Order(10)
    @DisplayName("Test hasValidPassword")
    void testHasValidPassword() {
        // Senza password
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, "");
        assertFalse(DBConfigStore.hasValidPassword());

        // Con password valida
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, TEST_PASSWORD);
        assertTrue(DBConfigStore.hasValidPassword());
    }

    @Test
    @Order(11)
    @DisplayName("Test caratteri speciali nella password")
    void testSpecialCharactersInPassword() {
        // Arrange
        String[] specialPasswords = {
                "!@#$%^&*()",
                "\\\"'`~<>?/",
                "Pass\nWith\nNewlines",
                "Pass\tWith\tTabs",
                "Password_With_Underscore_And_Numbers_123456789"
        };

        // Act & Assert
        for (String password : specialPasswords) {
            DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, password);
            assertEquals(password, DBConfigStore.getPassword(),
                    String.format("Caratteri speciali non gestiti correttamente in: %s", password));
            DBConfigStore.clearAll();
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test password molto lunghe")
    void testVeryLongPassword() {
        // Arrange - Genera password da 1KB
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            longPassword.append((char) ('A' + (i % 26)));
        }
        String testPassword = longPassword.toString();

        // Act
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, testPassword);
        String retrieved = DBConfigStore.getPassword();

        // Assert
        assertEquals(testPassword.length(), retrieved.length(),
                "Lunghezza password non corrisponde");
        assertEquals(testPassword, retrieved,
                "Password lunga non decifrata correttamente");
    }

    @Test
    @Order(13)
    @DisplayName("Test sicurezza: password cifrata non uguale a plaintext")
    void testPasswordIsEncrypted() {
        // Arrange & Act
        DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, TEST_PASSWORD);

        // Recupera direttamente dalle Preferences la password cifrata
        Preferences prefs = Preferences.userNodeForPackage(DBConfigStore.class);
        String encryptedPassword = prefs.get("db.pass", "");

        // Assert
        assertNotEquals("", encryptedPassword, "Password dovrebbe essere salvata");
        assertNotEquals(TEST_PASSWORD, encryptedPassword,
                "Password salvata NON deve essere in plaintext");
        assertTrue(encryptedPassword.length() > TEST_PASSWORD.length(),
                "Password cifrata dovrebbe essere più lunga (include IV e tag)");
    }

    @Test
    @Order(14)
    @DisplayName("Test multiple operazioni sequenziali")
    void testMultipleSequentialOperations() {
        // Simula uso reale con multiple operazioni
        for (int i = 0; i < 10; i++) {
            String password = "Password_" + i;
            DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, password);

            assertEquals(password, DBConfigStore.getPassword(),
                    String.format("Iterazione %d fallita", i));
        }
    }

    @Test
    @Order(15)
    @DisplayName("Test compatibilità Unicode completo")
    void testFullUnicodeSupport() {
        //Varie lingue e simboli
        String[] unicodePasswords = {
                "日本語パスワード",           // Giapponese
                "密碼測試中文",              // Cinese
                "Пароль_Русский",          // Russo
                "كلمة_المرور_العربية",      // Arabo
                "סיסמה_עברית",             // Ebraico
                "🔐🔑🛡️🔒💻",             // Emoji
                "Ελληνικό_Κωδικός"         // Greco
        };

        for (String password : unicodePasswords) {
            DBConfigStore.save(TEST_HOST, TEST_PORT, TEST_DB, TEST_USER, password);
            assertEquals(password, DBConfigStore.getPassword(),
                    String.format("Unicode non supportato per: %s", password));
            DBConfigStore.clearAll();
        }
    }
}