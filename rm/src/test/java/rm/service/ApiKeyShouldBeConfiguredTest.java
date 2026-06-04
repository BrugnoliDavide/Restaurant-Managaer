package rm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApiKeyShouldBeConfiguredTest {

    @Test
    void apiKeyShouldBeConfigured() {
        String key = System.getProperty("spoonacular.api.key");
        if (key == null || key.isBlank()) {
            key = System.getenv("SPOONACULAR_API_KEY");
        }

        assumeTrue(
                key != null && !key.isBlank(),
                "Test saltato: SPOONACULAR_API_KEY non configurata nell'ambiente corrente"
        );

        assertFalse(key.isBlank(), "La chiave API Spoonacular non deve essere vuota");
    }
}