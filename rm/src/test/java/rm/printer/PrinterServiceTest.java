package rm.printer;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrinterServiceTest {

    private PrinterService printerService;

    @BeforeEach
    void setUp() {
        printerService = PrinterService.getInstance();
    }

    @Test
    @Order(1)
    @DisplayName("Verifica pattern Singleton")
    void testSingletonPattern() {
        PrinterService instance1 = PrinterService.getInstance();
        PrinterService instance2 = PrinterService.getInstance();

        assertSame(instance1, instance2,
                "PrinterService deve essere un singleton");
    }

    @Test
    @Order(2)
    @DisplayName("Elenca stampanti disponibili sul sistema")
    void testGetAvailablePrinters() {
        String[] printers = printerService.getAvailablePrinters();

        assertNotNull(printers, "Array stampanti non deve essere null");

        // Log per debugging (utile durante lo sviluppo)
        System.out.println("Stampanti disponibili sul sistema:");
        for (String printer : printers) {
            System.out.println("  - " + printer);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Configurazione di default corretta")
    void testDefaultConfiguration() {
        PrinterConfiguration config = printerService.getConfiguration();

        assertNotNull(config, "Configurazione non deve essere null");
        assertFalse(config.isEnabled(),
                "Stampante dovrebbe essere disabilitata di default");
        assertEquals(48, config.getPrintWidth(),
                "Larghezza default dovrebbe essere 48 caratteri");
    }

    @Test
    @Order(4)
    @DisplayName("Aggiornamento configurazione")
    void testUpdateConfiguration() {
        PrinterConfiguration newConfig = new PrinterConfiguration();
        newConfig.setPrinterName("Test Printer");
        newConfig.setEnabled(true);
        newConfig.setPrintWidth(42);

        printerService.updateConfiguration(newConfig);

        PrinterConfiguration retrieved = printerService.getConfiguration();
        assertEquals("Test Printer", retrieved.getPrinterName());
        assertTrue(retrieved.isEnabled());
        assertEquals(42, retrieved.getPrintWidth());
    }
}