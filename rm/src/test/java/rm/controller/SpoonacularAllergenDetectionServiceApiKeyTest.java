package rm.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica che il costruttore di {@link SpoonacularAllergenDetectionService}
 * gestisca correttamente la presenza e assenza della chiave API.
 *
 * <p>Questi test usano il costruttore a due parametri (dependency injection),
 * indipendente dalle variabili d'ambiente della macchina.</p>
 */
class SpoonacularAllergenDetectionServiceApiKeyTest {

    @Test
    void shouldAcceptValidApiKey() {
        assertDoesNotThrow(
                () -> new SpoonacularAllergenDetectionService(
                        "valid-test-key-12345",
                        java.net.http.HttpClient.newHttpClient()));
    }

    @Test
    void shouldRejectNullApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        null,
                        java.net.http.HttpClient.newHttpClient()));
    }

    @Test
    void shouldRejectEmptyApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        "",
                        java.net.http.HttpClient.newHttpClient()));
    }

    @Test
    void shouldRejectBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        "   ",
                        java.net.http.HttpClient.newHttpClient()));
    }
}