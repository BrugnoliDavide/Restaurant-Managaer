package com.example.rm.util;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import static com.example.rm.view.WaiterController.logger;


public class TableSelectionUtils {

    private TableSelectionUtils() {
        // Lancia un'eccezione se qualcuno prova a istanziarla via Reflection
        throw new IllegalStateException("Utility class");
    }


    /**
     * Converte una stringa complessa (es: "1-5; 10; 12-14") in un Set di interi.
     * Se la stringa è vuota o nulla, restituisce null (che interpreteremo come "tutti i tavoli").
     */
    public static Set<Integer> parseTableString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new HashSet<>();
        }

        Set<Integer> tables = new HashSet<>();
        // Rimuoviamo spazi bianchi e dividiamo per il punto e virgola
        String[] parts = input.split(";");

        for (String part : parts) {
            part = part.trim();
            try {
                if (part.contains("-")) {
                    // Gestione range (es. "1-5")
                    String[] range = part.split("-");
                    if (range.length == 2) {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        // Gestione caso inverso (es. 5-1)
                        int min = Math.min(start, end);
                        int max = Math.max(start, end);
                        for (int i = min; i <= max; i++) {
                            tables.add(i);
                        }
                    }
                } else {
                    // Numero singolo
                    tables.add(Integer.parseInt(part));
                }
            } catch (NumberFormatException e) {

                logger.log(Level.WARNING, "errore paring tavoli:  {0}", part);
            }
        }
        return tables;
    }
}