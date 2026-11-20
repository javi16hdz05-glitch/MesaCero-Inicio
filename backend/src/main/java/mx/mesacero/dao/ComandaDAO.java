package mx.mesacero.dao;

import mx.mesacero.model.Comanda;
import mx.mesacero.model.ItemComanda;
import mx.mesacero.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Una comanda se guarda en dos tablas (comandas y comanda_items),
 * por eso crear/actualizar trabajan dentro de una transacción.
 * El total siempre lo calcula el servidor con los precios de la BD.
 */
public class ComandaDAO implements CrudDAO<Comanda> {

    private Comanda mapear(ResultSet rs) throws SQLException {
        Comanda c = new Comanda();
        c.setId(rs.getInt("id"));
        c.setMesaId(rs.getInt("mesa_id"));
        c.setTotal(rs.getDouble("total"));
        c.setFecha(rs.getTimestamp("fecha").toInstant().toString());
        c.setEstado(rs.getString("estado"));
        return c;
    }

    private List<ItemComanda> cargarItems(Connection cn, int comandaId) throws SQLException {
        List<ItemComanda> items = new ArrayList<>();
        String sql = "SELECT producto_id, cantidad FROM comanda_items WHERE comanda_id = ? ORDER BY id";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, comandaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new ItemComanda(rs.getInt("producto_id"), rs.getInt("cantidad")));
                }
            }
        }
        return items;
    }

    private double calcularTotal(Connection cn, List<ItemComanda> items) throws SQLException {
        double total = 0;
        String sql = "SELECT precio, disponible FROM productos WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (ItemComanda item : items) {
                ps.setInt(1, item.getProductoId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("No existe el producto " + item.getProductoId());
                    }
                    if (!rs.getBoolean("disponible")) {
                        throw new IllegalArgumentException("El producto " + item.getProductoId() + " no está disponible");
                    }
                    total += rs.getDouble("precio") * item.getCantidad();
                }
            }
        }
        return total;
    }

    private void insertarItems(Connection cn, int comandaId, List<ItemComanda> items) throws SQLException {
        String sql = "INSERT INTO comanda_items (comanda_id, producto_id, cantidad) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (ItemComanda item : items) {
                ps.setInt(1, comandaId);
                ps.setInt(2, item.getProductoId());
                ps.setInt(3, item.getCantidad());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public Comanda crear(Comanda c) throws SQLException {
        try (Connection cn = ConexionBD.getConexion()) {
            cn.setAutoCommit(false);
            try {
                c.setTotal(calcularTotal(cn, c.getItems()));
                String sql = "INSERT INTO comandas (mesa_id, total, estado) VALUES (?, ?, ?) RETURNING id, fecha";
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt(1, c.getMesaId());
                    ps.setDouble(2, c.getTotal());
                    ps.setString(3, c.getEstado());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        c.setId(rs.getInt("id"));
                        c.setFecha(rs.getTimestamp("fecha").toInstant().toString());
                    }
                }
                insertarItems(cn, c.getId(), c.getItems());
                try (PreparedStatement ps = cn.prepareStatement("UPDATE mesas SET estado = 'ocupada' WHERE id = ?")) {
                    ps.setInt(1, c.getMesaId());
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
        return c;
    }

    @Override
    public Comanda buscarPorId(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM comandas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Comanda c = mapear(rs);
                c.setItems(cargarItems(cn, c.getId()));
                return c;
            }
        }
    }

    @Override
    public List<Comanda> listar() throws SQLException {
        List<Comanda> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM comandas ORDER BY fecha DESC")) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            for (Comanda c : lista) {
                c.setItems(cargarItems(cn, c.getId()));
            }
        }
        return lista;
    }

    @Override
    public boolean actualizar(Comanda c) throws SQLException {
        try (Connection cn = ConexionBD.getConexion()) {
            cn.setAutoCommit(false);
            try {
                double total = calcularTotal(cn, c.getItems());
                String sql = "UPDATE comandas SET mesa_id = ?, estado = ?, total = ? WHERE id = ?";
                int filas;
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt(1, c.getMesaId());
                    ps.setString(2, c.getEstado());
                    ps.setDouble(3, total);
                    ps.setInt(4, c.getId());
                    filas = ps.executeUpdate();
                }
                if (filas == 0) {
                    cn.rollback();
                    return false;
                }
                try (PreparedStatement ps = cn.prepareStatement("DELETE FROM comanda_items WHERE comanda_id = ?")) {
                    ps.setInt(1, c.getId());
                    ps.executeUpdate();
                }
                insertarItems(cn, c.getId(), c.getItems());
                c.setTotal(total);
                cn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        // comanda_items se borra solo por ON DELETE CASCADE
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM comandas WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
