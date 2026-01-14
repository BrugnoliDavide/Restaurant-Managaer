package com.example.rm.controller;

import com.example.rm.dao.CategoryDAO;
import com.example.rm.dao.DatabaseCategoryDAO;
import com.example.rm.dao.ProductDAO;
import com.example.rm.dao.DatabaseProductDAO;
import com.example.rm.model.MenuProduct;

import java.util.List;

public class ProductController implements ProductUseCase {

    private final CategoryDAO categoryDAO;
    private final ProductDAO productDAO;

    // default per produzione
    public ProductController() {
        this(new DatabaseCategoryDAO(), new DatabaseProductDAO());
    }

    // per test/injection
    public ProductController(CategoryDAO categoryDAO, ProductDAO productDAO) {
        this.categoryDAO = categoryDAO;
        this.productDAO = productDAO;
    }

    @Override
    public List<String> getAllCategories() {
        return categoryDAO.getAllCategories();
    }

    @Override
    public boolean saveProduct(MenuProduct product) {
        if (product == null || product.getNome() == null || product.getNome().trim().isEmpty()) {
            return false;
        }
        return productDAO.save(product);
    }

    @Override
    public boolean deleteProduct(Long productId) {
        if (productId == null || productId <= 0) {
            return false;
        }
        return productDAO.delete(productId);
    }
}
