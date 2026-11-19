package mx.edu.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.mesacero.config.DatabasePool;
import mx.edu.mesacero.http.Api;
import mx.edu.mesacero.http.SqlErrors;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** Nombres de vistas fijados en lista blanca; nunca interpolar nombres arbitrarios. */
@WebServlet("/api/admin/reportes")
public final class ReportesServlet extends HttpServlet {
    private static final Set<String> VISTAS = Set.of("vw_reservas_detalle", "vw_cuenta_pedidos",
            "vw_pedidos_detalle", "vw_ventas_diarias", "vw_ventas_por_hora",
            "vw_productos_mas_vendidos", "vw_cortes_caja", "vw_dashboard_resumen");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String vista = req.getParameter("vista");
        if (vista == null || !VISTAS.contains(vista)) {
            Api.error(res, 400, "Vista no permitida. Opciones: " + String.join(", ", VISTAS));
            return;
        }
        var datos = new ArrayList<Map<String, Object>>();
        try (Connection conn = DatabasePool.conexion();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM mesacero." + vista + " LIMIT 200");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) datos.add(Api.row(rs));
            Api.send(res, 200, datos);
        } catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
