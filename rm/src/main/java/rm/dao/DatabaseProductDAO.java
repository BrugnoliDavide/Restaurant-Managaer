package rm.dao;

import rm.bean.ProductBean;
import rm.service.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione PostgreSQL di {@link ProductDAO}.
 *
 * <p>Tutti i metodi di lettura restituiscono {@link ProductBean}; la conversione
 * a {@code MenuProduct} (con JavaFX Properties) avviene nel layer Service
 * tramite {@code BeanMapper}.</p>
 */
public class DatabaseProductDAO implements ProductDAO {

    private static final Logger logger = Logger.getLogger(DatabaseProductDAO.class.getName());

    private static final String SELECT_COLS =
            "SELECT id, nome, tipologia, prezzo_vendita, costo_realizzazione, allergeni";

    // =========================================================================
    //  Lettura — restituisce ProductBean
    // =========================================================================

    @Override
    public List<ProductBean> findAll() {
        List<ProductBean> prodotti = new ArrayList<>();
        String sql = SELECT_COLS + " FROM menu_items";

        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                prodotti.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti", e);
        }
        return prodotti;
    }

    @Override
    public ProductBean findById(int id) {
        String sql = SELECT_COLS + " FROM menu_items WHERE id = ?";

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
                + "JOIN menu_items mi ON oi.menu_item_id = mi.id "
                + "WHERE mi.nome = ? AND oi.status <> 'canceled'";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeProdotto);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore conteggio vendite", e);
        }
        return 0;
    }

    // =========================================================================
    //  Scrittura — accetta ProductBean (conversione Model→Bean avviene nel Service)
    // =========================================================================

    @Override
    public boolean save(ProductBean product) {
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
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                logger.info("Prodotto eliminato con successo.");
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

    // =========================================================================
    //  Metodi privati
    // =========================================================================

    private boolean insert(ProductBean p) {
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

    private boolean update(ProductBean p) {
        String sql = "UPDATE menu_items SET nome=?, tipologia=?, prezzo_vendita=?, "
                + "costo_realizzazione=?, allergeni=? WHERE id=?";

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

    private void bindProduct(PreparedStatement pstmt, ProductBean p) throws SQLException {
        pstmt.setString(1, p.getNome());
        pstmt.setString(2, p.getTipologia());
        pstmt.setBigDecimal(3, p.getPrezzoVendita());
        pstmt.setBigDecimal(4, p.getCostoRealizzazione());
        pstmt.setString(5, p.getAllergeni());
    }

    /** Mappa la riga corrente del ResultSet in un ProductBean. */
    private ProductBean mapRow(ResultSet rs) throws SQLException {
        return new ProductBean(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("tipologia"),
                rs.getBigDecimal("prezzo_vendita"),
                rs.getBigDecimal("costo_realizzazione"),
                rs.getString("allergeni")
        );
    }
}