package rm.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpoonacularAllergenDetectionServiceApiKeyTest {

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    @org.junit.jupiter.api.Test
    void shouldAcceptValidApiKey() {
        assertDoesNotThrow(
                () -> new SpoonacularAllergenDetectionService(
                        "valid-test-key-12345",
                        HTTP_CLIENT));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectInvalidApiKey(String apiKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(
                        apiKey,
                        HTTP_CLIENT));
    }
}