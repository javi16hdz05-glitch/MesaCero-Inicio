package mx.edu.mesacero.servlet;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import java.util.Map;

/** Alta ATÓMICA de pedido + renglones; el precio lo elige el trigger PostgreSQL. */
@WebServlet("/api/admin/comandas")
public final class ComandasServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JsonNode body = Api.body(req);
            JsonNode mesa = body.get("mesaId");
            JsonNode items = body.get("items");
            if (mesa == null || !mesa.canConvertToLong() || mesa.longValue() <= 0 ||
                    items == null || !items.isArray() || items.isEmpty() || items.size() > 50)
                throw new Api.BadRequest(400, "Indica mesaId y de 1 a 50 items");
            for (JsonNode item : items) {
                if (!item.isObject() || !item.has("productoId") || !item.path("productoId").canConvertToLong() ||
                        item.path("productoId").longValue() <= 0 ||
                        !item.has("cantidad") || !item.path("cantidad").canConvertToInt() ||
                        item.path("cantidad").intValue() < 1 || item.path("cantidad").intValue() > 99)
                    throw new Api.BadRequest(400, "Cada item requiere productoId y cantidad entre 1 y 99");
            }
            String notas = body.hasNonNull("notas") ? body.get("notas").asText() : null;
            Map<String, Object> pedido;
            try (Connection conn = DatabasePool.conexion()) {
                conn.setAutoCommit(false);
                try {
                    long id;
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO mesacero.pedidos(mesa_id,notas) VALUES (?,?) RETURNING id")) {
                        stmt.setLong(1, mesa.longValue());
                        stmt.setString(2, notas);
                        try (ResultSet rs = stmt.executeQuery()) { rs.next(); id = rs.getLong(1); }
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO mesacero.pedido_detalles(pedido_id,producto_id,cantidad) VALUES (?,?,?)")) {
                        for (JsonNode item : items) {
                            stmt.setLong(1, id);
                            stmt.setLong(2, item.get("productoId").longValue());
                            stmt.setInt(3, item.get("cantidad").intValue());
                            stmt.executeUpdate();
                        }
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT * FROM mesacero.vw_cuenta_pedidos WHERE pedido_id=?")) {
                        stmt.setLong(1, id);
                        try (ResultSet rs = stmt.executeQuery()) { rs.next(); pedido = Api.row(rs); }
                    }
                    conn.commit();
                } catch (SQLException | RuntimeException ex) {
                    conn.rollback();
                    throw ex;
                }
            }
            Api.send(res, 201, pedido);
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
