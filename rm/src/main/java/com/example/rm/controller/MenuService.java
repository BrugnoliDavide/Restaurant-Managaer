package com.example.rm.controller;

import com.example.rm.dao.ProductDAO;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.dao.CategoryDAO;
import com.example.rm.dao.DatabaseCategoryDAO;
import com.example.rm.model.MenuProduct;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenuService implements MenuUseCase {

    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;

    private static final Logger logger = Logger.getLogger(MenuService.class.getName());

    public MenuService() {
        this(new DatabaseProductDAO(), new DatabaseCategoryDAO());
    }

    public MenuService(ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
    }

    @Override
    public List<MenuProduct> loadAllProducts() {
        try {
            return productDAO.findAll();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento prodotti", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> loadCategories() {
        return categoryDAO.getAllCategories();
    }

    @Override
    public boolean addProduct(MenuProduct product) {
        return productDAO.save(product);
    }

    @Override
    public boolean updateProduct(MenuProduct product) {
        return productDAO.save(product);
    }

    @Override
    public MenuProduct getProductById(int id) {
        return productDAO.findById(id);
    }
}
