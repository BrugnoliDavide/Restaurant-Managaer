package com.example.rm.dao;

import com.example.rm.dao.impl.PreferencesPrinterConfigurationDAO;
import com.example.rm.printer.PrinterConfiguration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrinterConfigurationDAOTest {

    private PrinterConfigurationDAO dao;

    @BeforeEach
    void setUp() {
        dao = new PreferencesPrinterConfigurationDAO();
    }

    @AfterEach
    void tearDown() {
        // Pulisce le preferenze dopo ogni test
        dao.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Caricamento configurazione di default")
    void testLoadDefault() {
        dao.reset(); // Assicura che non ci siano dati

        PrinterConfiguration config = dao.load();

        assertNotNull(config);
        assertEquals("Auto-detect", config.getPrinterName());
        assertFalse(config.isEnabled());
        assertEquals(48, config.getPrintWidth());
        assertTrue(config.isAutoCut());
        assertEquals(3, config.getFeedLines());
    }

    @Test
    @Order(2)
    @DisplayName("Salvataggio e ricaricamento configurazione")
    void testSaveAndLoad() {
        PrinterConfiguration config = new PrinterConfiguration();
        config.setPrinterName("Epson TM-T20III");
        config.setEnabled(true);
        config.setPrintWidth(42);
        config.setAutoCut(false);
        config.setFeedLines(5);

        boolean saved = dao.save(config);
        assertTrue(saved, "Salvataggio deve riuscire");

        PrinterConfiguration loaded = dao.load();

        assertEquals("Epson TM-T20III", loaded.getPrinterName());
        assertTrue(loaded.isEnabled());
        assertEquals(42, loaded.getPrintWidth());
        assertFalse(loaded.isAutoCut());
        assertEquals(5, loaded.getFeedLines());
    }

    @Test
    @Order(3)
    @DisplayName("Reset configurazione")
    void testReset() {
        // Salva una configurazione custom
        PrinterConfiguration config = new PrinterConfiguration();
        config.setPrinterName("Custom Printer");
        config.setEnabled(true);
        dao.save(config);

        // Reset
        boolean reset = dao.reset();
        assertTrue(reset);

        // Verifica che sia tornata ai default
        PrinterConfiguration loaded = dao.load();
        assertEquals("Auto-detect", loaded.getPrinterName());
        assertFalse(loaded.isEnabled());
    }
}