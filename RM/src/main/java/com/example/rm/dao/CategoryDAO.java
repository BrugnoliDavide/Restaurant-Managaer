package com.example.rm.dao;

import java.util.List;

public interface CategoryDAO {

    /**
     * Restituisce tutte le categorie di prodotto disponibili nel menu.
     */
    List<String> getAllCategories();
}
