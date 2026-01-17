package com.example.rm.preference;


import java.util.HashSet;

import java.util.Objects;
import java.util.Set;

/**
 * Gestisce le preferenze di un utente cucina.
 * Ogni cucina ha preferenze indipendenti.
 */
public class KitchenPreferences {

    private String username;  // Username dell'utente cucina

    /**
     * Se true: scompone ordini multi-categoria in ordini mono-categoria
     * Se false: mostra ordini così come sono
     */
    private boolean splitMixedCategoryOrders;

    /**
     * Categorie esplicitamente selezionate
     */
    private Set<String> selectedCategories;

    /**
     * Se true: include anche categorie NON nella lista selectedCategories
     * (utile per categorie aggiunte DOPO aver salvato le preferenze)
     * al momento non implementato
     */
    private boolean includeOtherCategories;

    public KitchenPreferences() {
        this.selectedCategories = new HashSet<>();
        this.splitMixedCategoryOrders = false;
        this.includeOtherCategories = false;
    }

    public KitchenPreferences(String username,
                              boolean splitMixedCategoryOrders,
                              Set<String> selectedCategories,
                              boolean includeOtherCategories) {
        this.username = username;
        this.splitMixedCategoryOrders = splitMixedCategoryOrders;
        this.selectedCategories = selectedCategories != null
                ? new HashSet<>(selectedCategories)
                : new HashSet<>();
        this.includeOtherCategories = includeOtherCategories;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSplitMixedCategoryOrders() {
        return splitMixedCategoryOrders;
    }

    public void setSplitMixedCategoryOrders(boolean splitMixedCategoryOrders) {
        this.splitMixedCategoryOrders = splitMixedCategoryOrders;
    }

    public Set<String> getSelectedCategories() {
        return new HashSet<>(selectedCategories);
    }

    public void setSelectedCategories(Set<String> selectedCategories) {
        this.selectedCategories = selectedCategories != null
                ? new HashSet<>(selectedCategories)
                : new HashSet<>();
    }

    public void addCategory(String category) {
        if (category != null && !category.trim().isEmpty()) {
            this.selectedCategories.add(category.trim());
        }
    }

    public void removeCategory(String category) {
        this.selectedCategories.remove(category);
    }

    public void clearCategories() {
        this.selectedCategories.clear();
    }

    public boolean isCategorySelected(String category) {
        return this.selectedCategories.contains(category);
    }

    public boolean isIncludeOtherCategories() {
        return includeOtherCategories;
    }

    public void setIncludeOtherCategories(boolean includeOtherCategories) {
        this.includeOtherCategories = includeOtherCategories;
    }



    /**
     * @param orderCategories Categorie dell'ordine
     * @return true se l'ordine rientra nelle preferenze
     */
    public boolean shouldDisplayOrder(Set<String> orderCategories) {
        return orderCategories != null
                && !orderCategories.isEmpty()
                && !selectedCategories.isEmpty()
                && (includeOtherCategories
                || orderCategories.stream().anyMatch(selectedCategories::contains));
    }


    @Override
    public String toString() {
        return "KitchenPreferences{" +
                "username='" + username + '\'' +
                ", splitMixedCategoryOrders=" + splitMixedCategoryOrders +
                ", selectedCategories=" + selectedCategories +
                ", includeOtherCategories=" + includeOtherCategories +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KitchenPreferences)) return false;
        KitchenPreferences that = (KitchenPreferences) o;
        return splitMixedCategoryOrders == that.splitMixedCategoryOrders
                && includeOtherCategories == that.includeOtherCategories
                && Objects.equals(username, that.username)
                && Objects.equals(selectedCategories, that.selectedCategories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, splitMixedCategoryOrders, selectedCategories, includeOtherCategories);
    }

}
