package mx.edu.mesacero.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Utilidades compartidas: JSON, validación de cuerpo y filas JDBC. */
public final class Api {
    public static final ObjectMapper JSON = new ObjectMapper();
    public static final int MAX_BODY = 64 * 1024;
    private Api() {}

    public static void send(HttpServletResponse res, int status, Object value) throws IOException {
        res.setStatus(status);
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json; charset=UTF-8");
        res.setHeader("Cache-Control", "no-store");
        JSON.writeValue(res.getWriter(), value);
    }

    public static void error(HttpServletResponse res, int status, String message) throws IOException {
        send(res, status, Map.of("error", message));
    }

    public static JsonNode body(HttpServletRequest req) throws IOException, BadRequest {
        String type = req.getContentType();
        if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("application/json"))
            throw new BadRequest(415, "Envía Content-Type: application/json");
        byte[] raw = req.getInputStream().readNBytes(MAX_BODY + 1);
        if (raw.length > MAX_BODY) throw new BadRequest(413, "El JSON excede 64 KiB");
        try {
            JsonNode node = JSON.readTree(new String(raw, StandardCharsets.UTF_8));
            if (node == null || !node.isObject()) throw new BadRequest(400, "Se espera un objeto JSON");
            return node;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new BadRequest(400, "JSON inválido");
        }
    }

    public static Map<String, Object> row(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int col = 1; col <= md.getColumnCount(); col++) {
            Object value = rs.getObject(col);
            if (value instanceof java.sql.Timestamp timestamp) value = timestamp.toInstant().toString();
            else if (value instanceof java.sql.Date date) value = date.toLocalDate().toString();
            else if (value instanceof OffsetDateTime dateTime) value = dateTime.toString();
            result.put(md.getColumnLabel(col), value);
        }
        return result;
    }

    public static long positiveId(String raw) throws BadRequest {
        try {
            long id = Long.parseLong(raw);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new BadRequest(400, "ID inválido; debe ser un número entero positivo");
    }

    public static int pageParam(String raw, int fallback, int max) throws BadRequest {
        if (raw == null) return fallback;
        try {
            int n = Integer.parseInt(raw);
            if (n >= 0 && n <= max) return n;
        } catch (NumberFormatException ignored) { }
        throw new BadRequest(400, "Paginación fuera de rango");
    }

    public static final class BadRequest extends Exception {
        public final int status;
        public BadRequest(int status, String message) { super(message); this.status = status; }
    }
}
