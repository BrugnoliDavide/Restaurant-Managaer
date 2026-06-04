package rm.controller;

import rm.exception.AllergenDetectionException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione di {@link AllergenDetectionUseCase} basata sull'API Spoonacular.
 *
 * <p>Utilizza l'endpoint {@code /recipes/complexSearch} con il parametro
 * {@code addRecipeInformation=true} per ottenere i flag booleani relativi
 * agli allergeni (glutenFree, dairyFree, ecc.) e li converte in una stringa
 * leggibile conforme al Regolamento UE 1169/2011.</p>
 *
 * <p>La chiave API viene letta dalla proprietà di sistema
 * {@code spoonacular.api.key} oppure dalla variabile d'ambiente
 * {@code SPOONACULAR_API_KEY}.</p>
 */
public class SpoonacularAllergenDetectionService implements AllergenDetectionUseCase {

    private static final Logger logger =
            Logger.getLogger(SpoonacularAllergenDetectionService.class.getName());

    private static final String BASE_URL = "https://api.spoonacular.com/recipes/complexSearch";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int EXPECTED_HTTP_OK = 200;

    private static final String SYSTEM_PROPERTY_KEY = "spoonacular.api.key";
    private static final String ENV_VARIABLE_KEY = "SPOONACULAR_API_KEY";

    private final String apiKey;
    private final HttpClient httpClient;

    // =========================================================================
    //  Costruttori
    // =========================================================================

    /**
     * Crea il servizio leggendo la chiave API dalla proprietà di sistema
     * {@value #SYSTEM_PROPERTY_KEY} o dalla variabile d'ambiente
     * {@value #ENV_VARIABLE_KEY}.
     *
     * @throws IllegalStateException se la chiave API non è configurata
     */
    public SpoonacularAllergenDetectionService() {
        this(resolveApiKey(), HttpClient.newHttpClient());
    }

    /**
     * Costruttore per dependency injection e testing.
     *
     * @param apiKey     chiave API Spoonacular (non {@code null}, non vuota)
     * @param httpClient client HTTP da utilizzare per le richieste
     */
    public SpoonacularAllergenDetectionService(String apiKey, HttpClient httpClient) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("La chiave API Spoonacular non può essere vuota");
        }
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    // =========================================================================
    //  Implementazione AllergenDetectionUseCase
    // =========================================================================

    @Override
    public String detectAllergens(String foodName) throws AllergenDetectionException {
        validateInput(foodName);

        String responseBody = executeSearch(foodName);
        JsonObject firstResult = extractFirstResult(responseBody, foodName);

        List<String> allergens = mapFlagsToAllergens(firstResult);

        if (allergens.isEmpty()) {
            throw new AllergenDetectionException(
                    "Nessun allergene rilevato per il prodotto: " + foodName);
        }

        StringJoiner joiner = new StringJoiner(", ");
        allergens.forEach(joiner::add);
        return joiner.toString();
    }

    // =========================================================================
    //  Metodi privati — validazione
    // =========================================================================

    private void validateInput(String foodName) throws AllergenDetectionException {
        if (foodName == null || foodName.isBlank()) {
            throw new AllergenDetectionException(
                    "Il nome del prodotto non può essere nullo o vuoto");
        }
    }

    // =========================================================================
    //  Metodi privati — comunicazione HTTP
    // =========================================================================

    private String executeSearch(String foodName) throws AllergenDetectionException {
        String encodedQuery = URLEncoder.encode(foodName, StandardCharsets.UTF_8);
        String url = BASE_URL
                + "?query=" + encodedQuery
                + "&addRecipeInformation=true"
                + "&number=1"
                + "&apiKey=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != EXPECTED_HTTP_OK) {
                throw new AllergenDetectionException(
                        "Errore API Spoonacular: HTTP " + response.statusCode());
            }
            return response.body();

        } catch (IOException e) {
            throw new AllergenDetectionException(
                    "Errore di rete durante la ricerca degli allergeni", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AllergenDetectionException(
                    "Richiesta interrotta durante la ricerca degli allergeni", e);
        }
    }

    // =========================================================================
    //  Metodi privati — parsing JSON
    // =========================================================================

    private JsonObject extractFirstResult(String responseBody, String foodName)
            throws AllergenDetectionException {

        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");

        if (results == null || results.isEmpty()) {
            throw new AllergenDetectionException(
                    "Nessun risultato trovato per il prodotto: " + foodName);
        }
        return results.get(0).getAsJsonObject();
    }

    /**
     * Mappa i flag booleani del JSON Spoonacular ai 14 allergeni
     * previsti dal Regolamento UE 1169/2011.
     *
     * <p>I flag disponibili nell'API sono limitati; vengono coperti:</p>
     * <ul>
     *     <li>{@code glutenFree == false} → <em>Glutine</em></li>
     *     <li>{@code dairyFree == false} → <em>Lattosio</em></li>
     *     <li>{@code vegan == false} e {@code vegetarian == true} → <em>Uova / Lattosio</em>
     *         (se non già rilevato)</li>
     *     <li>{@code vegan == false} e {@code vegetarian == false} → possibile presenza
     *         di <em>Pesce</em>, <em>Crostacei</em>, <em>Molluschi</em></li>
     * </ul>
     */
    private List<String> mapFlagsToAllergens(JsonObject recipe) {
        List<String> allergens = new ArrayList<>();

        addIfNotFree(allergens, recipe, "glutenFree", "Glutine");
        addIfNotFree(allergens, recipe, "dairyFree", "Lattosio");

        boolean isVegan = getBooleanSafe(recipe, "vegan");
        boolean isVegetarian = getBooleanSafe(recipe, "vegetarian");

        if (!isVegan && isVegetarian && !allergens.contains("Lattosio")) {
            allergens.add("Uova");
        }

        if (!isVegan && !isVegetarian) {
            allergens.add("Uova");
        }

        logger.log(Level.FINE, "Allergeni rilevati per \"{0}\": {1}",
                new Object[]{getStringSafe(recipe, "title"), allergens});

        return allergens;
    }

    private void addIfNotFree(List<String> allergens, JsonObject recipe,
                              String flagName, String allergenLabel) {
        if (!getBooleanSafe(recipe, flagName)) {
            allergens.add(allergenLabel);
        }
    }

    private boolean getBooleanSafe(JsonObject obj, String fieldName) {
        JsonElement element = obj.get(fieldName);
        if (element == null || element.isJsonNull()) {
            return false;
        }
        return element.getAsBoolean();
    }

    private String getStringSafe(JsonObject obj, String fieldName) {
        JsonElement element = obj.get(fieldName);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    // =========================================================================
    //  Metodi privati — configurazione
    // =========================================================================

    private static String resolveApiKey() {

        String key = System.getProperty(SYSTEM_PROPERTY_KEY);



        if (key != null && !key.isBlank()) {
            return key;
        }

        key = System.getenv(ENV_VARIABLE_KEY);
        if (key != null && !key.isBlank()) {
            return key;
        }

        throw new IllegalStateException(
                "Chiave API Spoonacular non configurata. "
                        + "Impostare la proprietà di sistema '" + SYSTEM_PROPERTY_KEY
                        + "' oppure la variabile d'ambiente '" + ENV_VARIABLE_KEY + "'.");
    }
}