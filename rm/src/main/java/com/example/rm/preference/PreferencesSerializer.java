package com.example.rm.preference;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PreferencesSerializer {

    private PreferencesSerializer() {
        // Non istanziabile
    }

    private static final String SPLIT_SEPARATOR = ",";
    private static final String KEY_VALUE_SEPARATOR = ":";
    private static final String CATEGORIES_SEPARATOR = ";";

    private static final Logger logger = Logger.getLogger(PreferencesSerializer.class.getName());


    /**
     * Serializza un oggetto KitchenPreferences in stringa.
     * @param prefs Preferenze da serializzare
     * @return Stringa serializzata (o null se prefs è null)
     */
    public static String serialize(KitchenPreferences prefs) {
        if (prefs == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        // splitOrders
        sb.append("splitOrders").append(KEY_VALUE_SEPARATOR)
                .append(prefs.isSplitMixedCategoryOrders()).append(SPLIT_SEPARATOR);

        // categories (join con ; per evitare conflitti con ,)
        Set<String> categories = prefs.getSelectedCategories();
        String categoriesStr = String.join(CATEGORIES_SEPARATOR, categories);
        sb.append("categories").append(KEY_VALUE_SEPARATOR)
                .append(categoriesStr).append(SPLIT_SEPARATOR);

        // includeOther
        sb.append("includeOther").append(KEY_VALUE_SEPARATOR)
                .append(prefs.isIncludeOtherCategories());

        return sb.toString();
    }

    /**
     * Deserializza una stringa in oggetto KitchenPreferences.
     * @param serialized Stringa serializzata
     * @param username Username per il quale creare le preferenze
     * @return KitchenPreferences deserializzate (o preferenze default se stringa invalida)
     */
    public static KitchenPreferences deserialize(String serialized, String username) {
        KitchenPreferences prefs = createDefaultPreferences(username);

        if (serialized == null || serialized.isBlank()) {
            return prefs;
        }

        try {
            for (String pair : serialized.split(SPLIT_SEPARATOR)) {
                parsePair(pair, prefs);
            }
            return prefs;
        } catch (Exception e) {
            return createDefaultPreferences(username);
        }
    }


    //metodi di supporto: necessari per ridurre la compelssità
    private static KitchenPreferences createDefaultPreferences(String username) {
        return new KitchenPreferences(
                username,
                PreferencesConstants.DEFAULT_SPLIT_ORDERS,
                new HashSet<>(),
                PreferencesConstants.DEFAULT_INCLUDE_OTHER
        );
    }

    private static void parsePair(String pair, KitchenPreferences prefs) {
        if (!pair.contains(KEY_VALUE_SEPARATOR)) {
            return;
        }

        String[] keyValue = pair.split(KEY_VALUE_SEPARATOR, 2);
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();

        switch (key) {
            case "splitOrders" -> prefs.setSplitMixedCategoryOrders(parseBoolean(value));
            case "categories"  -> prefs.setSelectedCategories(parseCategories(value));
            case "includeOther" -> prefs.setIncludeOtherCategories(parseBoolean(value));
            default -> logger.log(Level.WARNING, "chiave sconoscita: {0}", key);
        }

    }

    private static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    private static Set<String> parseCategories(String value) {
        if (value.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> categories = new HashSet<>();
        for (String cat : value.split(CATEGORIES_SEPARATOR)) {
            String trimmed = cat.trim();
            if (!trimmed.isEmpty()) {
                categories.add(trimmed);
            }
        }
        return categories;
    }


}
