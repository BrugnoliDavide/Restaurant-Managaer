package com.example.rm.controller;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.DatabaseService;
import java.util.List;

public class MenuController implements MenuUseCase {

    @Override
    public List<String> loadCategories() {
        return DatabaseService.getAllCategories();
    }

    @Override
    public boolean addProduct(MenuProduct product) {
        return DatabaseService.addProduct(product);
    }

    @Override
    public boolean updateProduct(MenuProduct product) {
        return DatabaseService.updateProduct(product);
    }

    @Override
    public MenuProduct getProductById(int id) {
        return DatabaseService.getProductById(id);
    }
}
