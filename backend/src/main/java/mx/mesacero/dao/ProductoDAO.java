package mx.mesacero.dao;

import mx.mesacero.model.Producto;
import mx.mesacero.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO implements CrudDAO<Producto> {

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setCategoria(rs.getString("categoria"));
        p.setPrecio(rs.getDouble("precio"));
        p.setDisponible(rs.getBoolean("disponible"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setFoto(rs.getString("foto"));
        p.setEtiqueta(rs.getString("etiqueta"));
        return p;
    }

    private void llenar(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.getNombre());
        ps.setString(2, p.getCategoria());
        ps.setDouble(3, p.getPrecio());
        ps.setBoolean(4, p.isDisponible());
        ps.setString(5, p.getDescripcion());
        ps.setString(6, p.getFoto());
        ps.setString(7, p.getEtiqueta());
    }

    @Override
    public Producto crear(Producto p) throws SQLException {
        String sql = "INSERT INTO productos (nombre, categoria, precio, disponible, descripcion, foto, etiqueta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            llenar(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                p.setId(rs.getInt(1));
            }
        }
        return p;
    }

    @Override
    public Producto buscarPorId(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Producto> listar() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM productos ORDER BY id")) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE productos SET nombre = ?, categoria = ?, precio = ?, disponible = ?, "
                + "descripcion = ?, foto = ?, etiqueta = ? WHERE id = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            llenar(ps, p);
            ps.setInt(8, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
