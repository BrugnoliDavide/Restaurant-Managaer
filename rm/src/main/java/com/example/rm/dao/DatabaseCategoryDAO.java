package com.example.rm.dao;

import com.example.rm.service.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DatabaseCategoryDAO implements CategoryDAO {

    private static final Logger logger = Logger.getLogger(DatabaseCategoryDAO.class.getName());

    // !! da eliminare
    /*@Override
    public List<String> getAllCategories() {
        return DatabaseService.getAllCategories();
    }/*/

    @Override
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT tipologia FROM menu_items ORDER BY tipologia";

        try (
            PreparedStatement pstmt =
                    DatabaseConnection.getConnection().prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(rs.getString(DatabaseService.returnTIPOLOGIASTRING()));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero categorie", e);
        }
        return categories;
    }




}
