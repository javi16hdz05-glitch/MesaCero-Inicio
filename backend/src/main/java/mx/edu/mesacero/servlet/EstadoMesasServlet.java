package mx.edu.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.mesacero.config.DatabasePool;
import mx.edu.mesacero.dto.MesaEstadoDto;
import mx.edu.mesacero.http.Api;
import mx.edu.mesacero.http.SqlErrors;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet("/api/estado-mesas")
public final class EstadoMesasServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var mesas = new ArrayList<MesaEstadoDto>();
        try (Connection conn = DatabasePool.conexion();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id,numero,capacidad,zona,fuera_servicio,estado " +
                "FROM mesacero.vw_estado_mesas ORDER BY numero");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) mesas.add(new MesaEstadoDto(
                    rs.getLong("id"), rs.getInt("numero"), rs.getInt("capacidad"),
                    rs.getString("zona"), rs.getBoolean("fuera_servicio"), rs.getString("estado")));
            Api.send(res, 200, mesas);
        } catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
