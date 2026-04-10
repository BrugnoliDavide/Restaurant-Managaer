package com.example.rm.dao;

import com.example.rm.model.MenuProduct;
import java.util.List;

public interface ProductDAO {

    List<MenuProduct> findAll();

    MenuProduct findById(int id);

    boolean save(MenuProduct product);

    boolean delete(Long productId);

    long getQuantitySold(String nomeProdotto);
}