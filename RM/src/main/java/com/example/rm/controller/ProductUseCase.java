package com.example.rm.controller;

import com.example.rm.model.MenuProduct;

import java.util.List;

public interface ProductUseCase {

    /**
     * Carica tutte le categorie disponibili.
     */
    List<String> getAllCategories();

    /**
     * Salva un nuovo prodotto o aggiorna uno esistente.
     *
     * @return true se salvato con successo
     */
    boolean saveProduct(MenuProduct product);

    /**
     * Elimina un prodotto per ID.
     *
     * @return true se eliminato con successo
     */
    boolean deleteProduct(Long productId);
}
