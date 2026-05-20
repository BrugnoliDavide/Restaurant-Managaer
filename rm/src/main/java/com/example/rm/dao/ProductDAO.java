package com.example.rm.dao;

import com.example.rm.bean.ProductBean;
import java.util.List;

/**
 * Data Access Object per i prodotti del menù.
 *
 * <p>I metodi di <em>lettura</em> restituiscono {@link ProductBean} (puro Java,
 * nessuna dipendenza JavaFX). La conversione a {@code MenuProduct} avviene
 * nel layer Service tramite {@code BeanMapper}.</p>
 *
 * <p>I metodi di <em>scrittura</em> accettano {@code ProductBean} in ingresso,
 * poiché il Service converte il Model prima di chiamare il DAO.</p>
 */
public interface ProductDAO {

    /** Tutti i prodotti del menù. */
    List<ProductBean> findAll();

    /**
     * Cerca un prodotto per ID.
     *
     * @return {@code ProductBean} oppure {@code null} se non trovato
     */
    ProductBean findById(int id);

    /**
     * Inserisce o aggiorna un prodotto (upsert basato su {@code id}).
     * Se {@code id <= 0} viene eseguito un INSERT, altrimenti un UPDATE.
     */
    boolean save(ProductBean product);

    /** Elimina un prodotto per ID. */
    boolean delete(Long productId);

    /** Quantità totale venduta di un prodotto (per nome). */
    long getQuantitySold(String nomeProdotto);
}