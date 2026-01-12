package com.example.rm.dao;

import com.example.rm.model.MenuProduct;

public interface ProductDAO {
    boolean save(MenuProduct product);
    boolean delete(Long productId);
}
