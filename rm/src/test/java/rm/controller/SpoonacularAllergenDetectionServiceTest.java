package rm.controller;

import rm.exception.AllergenDetectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test unitari per {@link SpoonacularAllergenDetectionService}.
 *
 * <p>Il {@link HttpClient} viene mockato per evitare chiamate di rete reali
 * e garantire determinismo nei test.</p>
 */
@ExtendWith(MockitoExtension.class)
class SpoonacularAllergenDetectionServiceTest {

    private static final String FAKE_API_KEY = "test-api-key-12345";

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private SpoonacularAllergenDetectionService service;

    @BeforeEach
    void setUp() {
        service = new SpoonacularAllergenDetectionService(FAKE_API_KEY, mockHttpClient);
    }


    @Test
    void detectAllergensShouldReturnGlutineAndLattosioWhenNotFree() throws Exception {
        String json = buildResponseJson(false, false, false, false);
        configureMockResponse(json, 200);

        String result = service.detectAllergens("Pasta Carbonara");

        assertTrue(result.contains("Glutine"));
        assertTrue(result.contains("Lattosio"));
    }

    @Test
    void detectAllergensShouldReturnOnlyGlutineWhenDairyFree() throws Exception {
        String json = buildResponseJson(false, false, false, true);
        configureMockResponse(json, 200);

        String result = service.detectAllergens("Pasta al pomodoro");

        assertTrue(result.contains("Glutine"));
    }

    @Test
    void detectAllergensShouldReturnUovaForVegetarianNonVegan() throws Exception {
        String json = buildResponseJson(true, false, true, true);
        configureMockResponse(json, 200);

        String result = service.detectAllergens("Frittata");

        assertTrue(result.contains("Uova"));
    }

    @Test
    void detectAllergensShouldReturnUovaForNonVegetarianNonVegan() throws Exception {
        String json = buildResponseJson(false, false, true, true);
        configureMockResponse(json, 200);

        String result = service.detectAllergens("Bistecca");

        assertTrue(result.contains("Uova"));
    }



    @Test
    void detectAllergensShouldThrowWhenNoResultsFound() throws Exception {
        String emptyJson = "{\"results\":[],\"totalResults\":0}";
        configureMockResponse(emptyJson, 200);

        AllergenDetectionException ex = assertThrows(
                AllergenDetectionException.class,
                () -> service.detectAllergens("CiboCheNonEsiste123"));

        assertTrue(ex.getMessage().contains("Nessun risultato"));
    }

    @Test
    void detectAllergensShouldThrowWhenVeganProductHasNoAllergens() throws Exception {
        String json = buildResponseJson(true, true, true, true);
        configureMockResponse(json, 200);

        AllergenDetectionException ex = assertThrows(
                AllergenDetectionException.class,
                () -> service.detectAllergens("Insalata mista"));

        assertTrue(ex.getMessage().contains("Nessun allergene rilevato"));
    }

    @Test
    void detectAllergensShouldThrowWhenFoodNameIsNull() {
        assertThrows(AllergenDetectionException.class,
                () -> service.detectAllergens(null));
    }

    @Test
    void detectAllergensShouldThrowWhenFoodNameIsBlank() {
        assertThrows(AllergenDetectionException.class,
                () -> service.detectAllergens("   "));
    }

    @Test
    void detectAllergensShouldThrowOnHttpError() throws Exception {
        // body() non viene stubato: con status != 200 il codice production
        // lancia AllergenDetectionException prima di chiamare response.body(),
        // evitando UnnecessaryStubbingException in strict mode.
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        AllergenDetectionException ex = assertThrows(
                AllergenDetectionException.class,
                () -> service.detectAllergens("Pizza"));

        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void detectAllergensShouldThrowOnNetworkError() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        AllergenDetectionException ex = assertThrows(
                AllergenDetectionException.class,
                () -> service.detectAllergens("Pizza"));

        assertTrue(ex.getMessage().contains("Errore di rete"));
    }



    @Test
    void constructorShouldRejectNullApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService(null, mockHttpClient));
    }

    @Test
    void constructorShouldRejectBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpoonacularAllergenDetectionService("  ", mockHttpClient));
    }


    @Test
    void detectAllergensShouldReturnCommaSeparatedString() throws Exception {
        String json = buildResponseJson(false, false, false, false);
        configureMockResponse(json, 200);

        String result = service.detectAllergens("Lasagna");

        assertEquals("Glutine, Lattosio, Uova", result);
    }


    @SuppressWarnings("unchecked")
    private void configureMockResponse(String body, int statusCode) throws Exception {
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(body);
        when(mockHttpClient.send(any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
    }

    private String buildResponseJson(boolean vegetarian, boolean vegan,
                                     boolean glutenFree, boolean dairyFree) {
        return "{\"results\":[{"
                + "\"id\":12345,"
                + "\"title\":\"Test Recipe\","
                + "\"vegetarian\":" + vegetarian + ","
                + "\"vegan\":" + vegan + ","
                + "\"glutenFree\":" + glutenFree + ","
                + "\"dairyFree\":" + dairyFree
                + "}],\"totalResults\":1}";
    }
}