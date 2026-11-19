package mx.edu.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.mesacero.config.DatabasePool;
import mx.edu.mesacero.http.Api;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/health")
public final class HealthServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (Connection conn = DatabasePool.conexion();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
            stmt.executeQuery();
            Api.send(res, 200, Map.of("estado", "ok", "servicio", "Mesa Cero API"));
        } catch (SQLException ex) {
            Api.error(res, 503, "Base de datos no disponible");
        }
    }
}
