package com.example.rm.dao;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;

public class DatabaseProductDAO implements ProductDAO {

    @Override
    public boolean save(MenuProduct product) {
        if (product == null) {
            return false;
        }
        // Se id <= 0 consideriamo il prodotto come nuovo
        if (product.getId() <= 0) {
            return DatabaseService.addProduct(product);
        } else {
            return DatabaseService.updateProduct(product);
        }
    }

    @Override
    public boolean delete(Long productId) {
        if (productId == null || productId <= 0) {
            return false;
        }
        // DatabaseService.deleteProduct richiede un int perchiò "l'asserzione"
        return DatabaseService.deleteProduct(productId.intValue());
    }
}
