package com.example.rm.dao;

import com.example.rm.model.MenuProduct;
import com.example.rm.service.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseProductDAO implements ProductDAO {

    private static final Logger logger = Logger.getLogger(DatabaseProductDAO.class.getName());

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Override
    public List<MenuProduct> findAll() {
        List<MenuProduct> prodotti = new ArrayList<>();
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, "
                + "costo_realizzazione, allergeni FROM menu_items";

        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                prodotti.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti", e);
        }
        return prodotti;
    }

    @Override
    public MenuProduct findById(int id) {
        String sql = "SELECT id, nome, tipologia, prezzo_vendita, "
                + "costo_realizzazione, allergeni FROM menu_items WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore recupero prodotto con ID {0}", id);
        }
        return null;
    }

    @Override
    public long getQuantitySold(String nomeProdotto) {
        String sql = "SELECT SUM(oi.quantita) FROM order_items oi "
                + "JOIN menu_items mi ON oi.menu_item_id = mi.id WHERE mi.nome = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeProdotto);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore conteggio vendite", e);
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Persistenza
    // -------------------------------------------------------------------------

    @Override
    public boolean save(MenuProduct product) {
        if (product == null) return false;
        return (product.getId() <= 0) ? insert(product) : update(product);
    }

    @Override
    public boolean delete(Long productId) {
        if (productId == null || productId <= 0) return false;

        logger.log(Level.INFO, "Tentativo eliminazione prodotto ID: {0}", productId);

        String sql = "DELETE FROM menu_items WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, productId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("SUCCESSO: Prodotto eliminato.");
                return true;
            } else {
                logger.log(Level.WARNING, "Nessuna riga trovata con ID: {0}", productId);
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore SQL durante eliminazione", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Metodi privati
    // -------------------------------------------------------------------------

    private boolean insert(MenuProduct p) {
        String sql = "INSERT INTO menu_items (nome, tipologia, prezzo_vendita, "
                + "costo_realizzazione, allergeni) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bindProduct(pstmt, p);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore inserimento prodotto", e);
            return false;
        }
    }

    private boolean update(MenuProduct p) {
        String sql = "UPDATE menu_items SET nome = ?, tipologia = ?, prezzo_vendita = ?, "
                + "costo_realizzazione = ?, allergeni = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bindProduct(pstmt, p);
            pstmt.setInt(6, p.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore aggiornamento prodotto", e);
            return false;
        }
    }

    private void bindProduct(PreparedStatement pstmt, MenuProduct p) throws SQLException {
        pstmt.setString(1, p.getNome());
        pstmt.setString(2, p.getTipologia());
        pstmt.setBigDecimal(3, p.getPrezzoVendita());
        pstmt.setBigDecimal(4, p.getCostoRealizzazione());
        pstmt.setString(5, p.getAllergeni());
    }

    private MenuProduct mapRow(ResultSet rs) throws SQLException {
        return new MenuProduct(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("tipologia"),
                rs.getBigDecimal("prezzo_vendita"),
                rs.getBigDecimal("costo_realizzazione"),
                rs.getString("allergeni")
        );
    }
}