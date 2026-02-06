package com.example.rm.dao;

import com.example.rm.model.MenuProduct;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseProductDAO implements ProductDAO {

    private static final Logger logger = Logger.getLogger(DatabaseProductDAO.class.getName());

    @Override
    public boolean save(MenuProduct product) {
        if (product == null) {
            return false;
        }
        // Se id <= 0 consideriamo il prodotto come nuovo
        if (product.getId() <= 0) {
            return addProduct(product);
        } else {
            return updateProduct(product);
        }
    }

    @Override
    public boolean delete(Long productId) {
        if (productId == null || productId <= 0) {
            return false;
        }
        return deleteProduct(productId.intValue());
    }




    public static boolean addProduct(MenuProduct p) {
        String sql = "INSERT INTO menu_items (nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt =
                     DatabaseConnection.getConnection().prepareStatement(sql))
        {

            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getTipologia());
            pstmt.setDouble(3, p.getPrezzoVendita());
            pstmt.setDouble(4, p.getCostoRealizzazione());
            pstmt.setString(5, p.getAllergeni());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE INSERIMENTO PRODOTTO", e);
            return false;
        }
    }

    public static boolean updateProduct(MenuProduct p) {
        String sql = "UPDATE menu_items SET nome = ?, tipologia = ?, prezzo_vendita = ?, costo_realizzazione = ?, allergeni = ? WHERE id = ?";

        try (PreparedStatement pstmt =
                     DatabaseConnection.getConnection().prepareStatement(sql))
        {
            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getTipologia());
            pstmt.setDouble(3, p.getPrezzoVendita());
            pstmt.setDouble(4, p.getCostoRealizzazione());
            pstmt.setString(5, p.getAllergeni());
            pstmt.setInt(6, p.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE UPDATE PRODOTTO", e);
            return false;
        }
    }

    public static boolean deleteProduct(int id) {
        logger.log(Level.INFO, "Tentativo eliminazione prodotto ID: {0}", id);

        if (id <= 0) {
            logger.log(Level.WARNING, "ID non valido per eliminazione: {0}", id);
            return false;
        }

        String sql = "DELETE FROM menu_items WHERE id = ?";

        try (PreparedStatement pstmt =
                     DatabaseConnection.getConnection().prepareStatement(sql))
        {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("SUCCESSO: Prodotto eliminato.");
                return true;
            } else {
                logger.log(Level.WARNING, "FALLIMENTO: Nessuna riga trovata con ID: {0}", id);
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "ERRORE SQL durante eliminazione: {0}, {1}", new Object[]{e.getMessage(), e});
            return false;
        }
    }
}

