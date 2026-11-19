package mx.edu.mesacero.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.ServletException;
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

/** CRUD de clave primaria compuesta: /carrito-items/{carritoId}/{productoId}. */
@WebServlet("/api/admin/carrito-items/*")
public final class CarritoItemsServlet extends HttpServlet {
    @Override protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if ("PATCH".equals(req.getMethod())) doPut(req, res);
        else super.service(req, res);
    }

    private long[] key(HttpServletRequest req) throws Api.BadRequest {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) return null;
        if (!path.matches("/[0-9]+/[0-9]+"))
            throw new Api.BadRequest(400, "Usa /carrito-items/{carritoId}/{productoId}");
        String[] parts = path.substring(1).split("/");
        return new long[]{Api.positiveId(parts[0]), Api.positiveId(parts[1])};
    }

    private long[] requiredKey(HttpServletRequest req) throws Api.BadRequest {
        long[] ids = key(req);
        if (ids == null) throw new Api.BadRequest(400, "Faltan carritoId y productoId en la ruta");
        return ids;
    }

    private int qty(JsonNode node) throws Api.BadRequest {
        JsonNode n = node.get("cantidad");
        if (n == null || !n.canConvertToInt() || n.intValue() < 1 || n.intValue() > 99)
            throw new Api.BadRequest(400, "cantidad debe ser un entero entre 1 y 99");
        return n.intValue();
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            long[] ids = key(req);
            String sql = "SELECT carrito_id,producto_id,cantidad FROM mesacero.carrito_items" +
                    (ids == null ? " ORDER BY carrito_id,producto_id LIMIT 100" : " WHERE carrito_id=? AND producto_id=?");
            try (Connection conn = DatabasePool.conexion(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (ids != null) { stmt.setLong(1, ids[0]); stmt.setLong(2, ids[1]); }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (ids != null) {
                        if (rs.next()) Api.send(res, 200, Api.row(rs));
                        else Api.error(res, 404, "Item no encontrado");
                    } else {
                        var out = new ArrayList<Map<String, Object>>();
                        while (rs.next()) out.add(Api.row(rs));
                        Api.send(res, 200, out);
                    }
                }
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            if (key(req) != null) throw new Api.BadRequest(405, "POST requiere la colección sin clave");
            JsonNode body = Api.body(req);
            JsonNode carrito = body.get("carritoId"), producto = body.get("productoId");
            if (carrito == null || !carrito.canConvertToLong() || carrito.longValue() <= 0 ||
                    producto == null || !producto.canConvertToLong() || producto.longValue() <= 0)
                throw new Api.BadRequest(400, "Requiere carritoId y productoId positivos");
            try (Connection conn = DatabasePool.conexion(); PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mesacero.carrito_items(carrito_id,producto_id,cantidad) " +
                    "VALUES (?,?,?) RETURNING *")) {
                stmt.setLong(1, carrito.longValue()); stmt.setLong(2, producto.longValue());
                stmt.setInt(3, qty(body));
                try (ResultSet rs = stmt.executeQuery()) { rs.next(); Api.send(res, 201, Api.row(rs)); }
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            long[] ids = requiredKey(req);
            int quantity = qty(Api.body(req));
            try (Connection conn = DatabasePool.conexion(); PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE mesacero.carrito_items SET cantidad=? WHERE carrito_id=? AND producto_id=? RETURNING *")) {
                stmt.setInt(1, quantity); stmt.setLong(2, ids[0]); stmt.setLong(3, ids[1]);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) Api.send(res, 200, Api.row(rs));
                    else Api.error(res, 404, "Item no encontrado");
                }
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            long[] ids = requiredKey(req);
            try (Connection conn = DatabasePool.conexion(); PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM mesacero.carrito_items WHERE carrito_id=? AND producto_id=?")) {
                stmt.setLong(1, ids[0]); stmt.setLong(2, ids[1]);
                if (stmt.executeUpdate() == 0) Api.error(res, 404, "Item no encontrado");
                else res.setStatus(204);
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
