package com.example.rm.dao;

import com.example.rm.service.DatabaseService;

import java.util.List;

public class DatabaseCategoryDAO implements CategoryDAO {

    @Override
    public List<String> getAllCategories() {
        return DatabaseService.getAllCategories();
    }
}
