package com.example.rm.controller;

import com.example.rm.dao.ProductDAO;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.dao.CategoryDAO;
import com.example.rm.dao.DatabaseCategoryDAO;
import com.example.rm.model.MenuProduct;
import com.example.rm.util.BeanMapper;

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
            // DAO restituisce List<ProductBean> → mappiamo a List<MenuProduct>
            return BeanMapper.toProductModels(productDAO.findAll());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento prodotti", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean addProduct(MenuProduct product) {
        // Model → Bean prima di passare al DAO
        return productDAO.save(BeanMapper.toBean(product));
    }

    @Override
    public boolean updateProduct(MenuProduct product) {
        return productDAO.save(BeanMapper.toBean(product));
    }

    @Override
    public MenuProduct getProductById(int id) {
        // DAO restituisce ProductBean → mappiamo a MenuProduct
        return BeanMapper.toModel(productDAO.findById(id));
    }


    @Override
    public List<String> loadCategories() {
        return categoryDAO.getAllCategories();
    }
}
