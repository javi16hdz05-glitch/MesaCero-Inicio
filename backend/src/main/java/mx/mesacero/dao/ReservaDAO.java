package mx.mesacero.dao;

import mx.mesacero.model.Reserva;
import mx.mesacero.util.ConexionBD;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ReservaDAO implements CrudDAO<Reserva> {

    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setId(rs.getInt("id"));
        r.setMesaId(rs.getInt("mesa_id"));
        r.setFecha(rs.getDate("fecha").toString());
        r.setHora(rs.getTime("hora").toString().substring(0, 5));
        r.setPersonas(rs.getInt("personas"));
        r.setZona(rs.getString("zona"));
        return r;
    }

    private Time convertirHora(String hora) {
        return Time.valueOf(hora.length() == 5 ? hora + ":00" : hora);
    }

    @Override
    public Reserva crear(Reserva r) throws SQLException {
        String sql = "INSERT INTO reservas (mesa_id, fecha, hora, personas, zona) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, r.getMesaId());
            ps.setDate(2, Date.valueOf(r.getFecha()));
            ps.setTime(3, convertirHora(r.getHora()));
            ps.setInt(4, r.getPersonas());
            ps.setString(5, r.getZona());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                r.setId(rs.getInt(1));
            }
        }
        return r;
    }

    @Override
    public Reserva buscarPorId(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM reservas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Reserva> listar() throws SQLException {
        List<Reserva> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM reservas ORDER BY fecha DESC, hora DESC")) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public boolean actualizar(Reserva r) throws SQLException {
        String sql = "UPDATE reservas SET mesa_id = ?, fecha = ?, hora = ?, personas = ?, zona = ? WHERE id = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, r.getMesaId());
            ps.setDate(2, Date.valueOf(r.getFecha()));
            ps.setTime(3, convertirHora(r.getHora()));
            ps.setInt(4, r.getPersonas());
            ps.setString(5, r.getZona());
            ps.setInt(6, r.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM reservas WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
