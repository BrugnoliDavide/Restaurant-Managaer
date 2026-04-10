package com.example.rm.service;

import com.example.rm.dao.DatabaseKitchenPreferencesDAO;
import com.example.rm.dao.KitchenPreferencesDAO;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.preference.KitchenPreferences;

import java.util.*;

/**
 * Gestisce le preferenze di cucina e il filtraggio degli ordini attivi
 * in base alle categorie selezionate dall'utente cucina.
 */
public final class KitchenService {

    private static final KitchenPreferencesDAO preferencesDAO =
            new DatabaseKitchenPreferencesDAO();

    private KitchenService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Recupera le preferenze cucina per un utente.
     */
    public static KitchenPreferences getPreferences(String username) {
        return preferencesDAO.loadByUsername(username);
    }

    /**
     * Restituisce gli ordini attivi filtrati secondo le preferenze dell'utente cucina.
     *
     * <p>Se l'utente ha attivato {@code includeOtherCategories}, vengono restituiti
     * tutti gli ordini attivi. Altrimenti vengono inclusi solo gli ordini i cui
     * articoli appartengono interamente alle categorie selezionate.</p>
     *
     * @param username username dell'utente cucina
     * @return lista di ordini filtrati
     */
    public static List<Order> getActiveOrdersFiltered(String username) {
        KitchenPreferences prefs = getPreferences(username);
        List<Order> allOrders = OrderService.getKitchenActive();

        if (prefs.isIncludeOtherCategories()) {
            return allOrders;
        }

        Set<String> selected = prefs.getSelectedCategories();
        List<Order> filtered = new ArrayList<>();

        for (Order order : allOrders) {
            List<OrderItem> items = OrderService.getItemsDetailed(order.getId());
            if (categoriesOf(items).stream().allMatch(selected::contains)) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    /**
     * Estrae l'insieme delle categorie (tipologie) presenti negli articoli.
     */
    private static Set<String> categoriesOf(List<OrderItem> items) {
        Set<String> categories = new HashSet<>();
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getProduct() != null && item.getProduct().getTipologia() != null) {
                    categories.add(item.getProduct().getTipologia());
                }
            }
        }
        return categories;
    }
}