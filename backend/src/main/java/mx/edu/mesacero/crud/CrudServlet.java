package mx.edu.mesacero.crud;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.mesacero.config.DatabasePool;
import mx.edu.mesacero.http.Api;
import mx.edu.mesacero.http.SqlErrors;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** GET, POST, PUT, PATCH y DELETE comunes a los diez Servlets administrativos. */
public abstract class CrudServlet extends HttpServlet {
    protected abstract TableSpec spec();

    @Override protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if ("PATCH".equals(req.getMethod())) doPatch(req, res);
        else super.service(req, res);
    }

    private Long optionalId(HttpServletRequest req) throws Api.BadRequest {
        String path = req.getPathInfo();
        if (path == null || path.equals("/") || path.isEmpty()) return null;
        if (!path.matches("/[0-9]+")) throw new Api.BadRequest(400, "Ruta de recurso inválida");
        return Api.positiveId(path.substring(1));
    }

    private long requiredId(HttpServletRequest req) throws Api.BadRequest {
        Long id = optionalId(req);
        if (id == null) throw new Api.BadRequest(400, "Indica el ID en la ruta, por ejemplo /7");
        return id;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            Long id = optionalId(req);
            if (id != null) {
                try (Connection conn = DatabasePool.conexion();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + spec().table + " WHERE id = ?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) Api.send(res, 200, Api.row(rs));
                        else Api.error(res, 404, "Registro no encontrado");
                    }
                }
            } else {
                int limit = Api.pageParam(req.getParameter("limit"), 50, 100);
                int offset = Api.pageParam(req.getParameter("offset"), 0, 1_000_000);
                List<Map<String, Object>> items = new ArrayList<>();
                try (Connection conn = DatabasePool.conexion();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + spec().table +
                             " ORDER BY id LIMIT ? OFFSET ?")) {
                    stmt.setInt(1, limit); stmt.setInt(2, offset);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) items.add(Api.row(rs));
                    }
                }
                Api.send(res, 200, Map.of("datos", items, "limit", limit, "offset", offset));
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            if (optionalId(req) != null) throw new Api.BadRequest(405, "POST se envía a la colección, sin ID");
            JsonNode body = Api.body(req);
            List<String> cols = checkedColumns(body, false);
            String statement = cols.isEmpty() ? "INSERT INTO " + spec().table + " DEFAULT VALUES RETURNING *"
                    : "INSERT INTO " + spec().table + " (" + String.join(",", cols) + ") VALUES (" +
                    "?,".repeat(cols.size()).replaceAll(",$", "") + ") RETURNING *";
            try (Connection conn = DatabasePool.conexion();
                 PreparedStatement stmt = conn.prepareStatement(statement)) {
                bind(stmt, body, cols);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    res.setHeader("Location", req.getRequestURI() + "/" + rs.getLong("id"));
                    Api.send(res, 201, Api.row(rs));
                }
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        update(req, res, true);
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse res) throws IOException {
        update(req, res, false);
    }

    private void update(HttpServletRequest req, HttpServletResponse res, boolean full) throws IOException {
        try {
            long id = requiredId(req);
            JsonNode body = Api.body(req);
            List<String> cols = checkedColumns(body, full);
            if (cols.isEmpty()) throw new Api.BadRequest(400, "Envía al menos un campo para actualizar");
            List<String> updates = new ArrayList<>();
            for (String col : cols) updates.add(col + " = ?");
            String sql = "UPDATE " + spec().table + " SET " + String.join(", ", updates) +
                    " WHERE id = ? RETURNING *";
            try (Connection conn = DatabasePool.conexion(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                bind(stmt, body, cols);
                stmt.setLong(cols.size() + 1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) Api.send(res, 200, Api.row(rs));
                    else Api.error(res, 404, "Registro no encontrado");
                }
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            long id = requiredId(req);
            try (Connection conn = DatabasePool.conexion();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + spec().table + " WHERE id = ?")) {
                stmt.setLong(1, id);
                if (stmt.executeUpdate() == 0) Api.error(res, 404, "Registro no encontrado");
                else res.setStatus(204);
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    private List<String> checkedColumns(JsonNode body, boolean full) throws Api.BadRequest {
        List<String> cols = new ArrayList<>();
        var fields = body.fieldNames();
        while (fields.hasNext()) {
            String col = fields.next();
            if (!spec().writable.containsKey(col))
                throw new Api.BadRequest(400, "Campo prohibido o desconocido: " + col);
            cols.add(col);
        }
        if (full && cols.size() != spec().writable.size())
            throw new Api.BadRequest(400, "PUT requiere todos los campos editables; para cambios parciales usa PATCH");
        return cols;
    }

    private void bind(PreparedStatement stmt, JsonNode body, List<String> cols)
            throws SQLException, Api.BadRequest {
        for (int i = 0; i < cols.size(); i++) {
            String col = cols.get(i);
            JsonNode value = body.get(col);
            if (value.isNull()) { stmt.setObject(i + 1, null); continue; }
            Object converted;
            String type = spec().writable.get(col);
            try {
                converted = switch (type) {
                    case "text" -> {
                        if (!value.isTextual()) throw new IllegalArgumentException();
                        yield value.asText();
                    }
                    case "bool" -> {
                        if (!value.isBoolean()) throw new IllegalArgumentException();
                        yield value.booleanValue();
                    }
                    case "int" -> {
                        if (!value.canConvertToInt()) throw new IllegalArgumentException();
                        yield value.intValue();
                    }
                    case "long" -> {
                        if (!value.canConvertToLong()) throw new IllegalArgumentException();
                        yield value.longValue();
                    }
                    case "decimal" -> {
                        if (!value.isNumber()) throw new IllegalArgumentException();
                        yield new BigDecimal(value.asText());
                    }
                    case "timestamp" -> {
                        if (!value.isTextual()) throw new IllegalArgumentException();
                        yield OffsetDateTime.parse(value.asText());
                    }
                    default -> throw new IllegalStateException("Tipo de configuración desconocido");
                };
            } catch (IllegalArgumentException | DateTimeParseException ex) {
                throw new Api.BadRequest(400, "Tipo o formato incorrecto en: " + col);
            }
            stmt.setObject(i + 1, converted);
        }
    }
}
