package mx.mesacero.dao;

import mx.mesacero.model.Mesa;
import mx.mesacero.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MesaDAO implements CrudDAO<Mesa> {

    private Mesa mapear(ResultSet rs) throws SQLException {
        return new Mesa(rs.getInt("id"), rs.getInt("numero"),
                rs.getInt("capacidad"), rs.getString("estado"));
    }

    @Override
    public Mesa crear(Mesa m) throws SQLException {
        String sql = "INSERT INTO mesas (numero, capacidad, estado) VALUES (?, ?, ?) RETURNING id";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, m.getNumero());
            ps.setInt(2, m.getCapacidad());
            ps.setString(3, m.getEstado());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                m.setId(rs.getInt(1));
            }
        }
        return m;
    }

    @Override
    public Mesa buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM mesas WHERE id = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Mesa> listar() throws SQLException {
        List<Mesa> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM mesas ORDER BY numero")) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public boolean actualizar(Mesa m) throws SQLException {
        String sql = "UPDATE mesas SET numero = ?, capacidad = ?, estado = ? WHERE id = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, m.getNumero());
            ps.setInt(2, m.getCapacidad());
            ps.setString(3, m.getEstado());
            ps.setInt(4, m.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Solo cambia el estado (lo usan las reservaciones). */
    public boolean actualizarEstado(int id, String estado) throws SQLException {
        String sql = "UPDATE mesas SET estado = ? WHERE id = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM mesas WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
