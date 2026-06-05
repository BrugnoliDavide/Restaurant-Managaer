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
        var httpClient = java.net.http.HttpClient.newHttpClient();

        assertDoesNotThrow(
                () -> new SpoonacularAllergenDetectionService(
                        "valid-test-key-12345",
                        httpClient));
    }

    @Test
    void shouldRejectNullApiKey() {
        var httpClient = java.net.http.HttpClient.newHttpClient();

        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        null,
                        httpClient));
    }

    @Test
    void shouldRejectEmptyApiKey() {
        var httpClient = java.net.http.HttpClient.newHttpClient();

        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        "",
                        httpClient));
    }

    @Test
    void shouldRejectBlankApiKey() {
        var httpClient = java.net.http.HttpClient.newHttpClient();

        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        "   ",
                        httpClient));
    }
}