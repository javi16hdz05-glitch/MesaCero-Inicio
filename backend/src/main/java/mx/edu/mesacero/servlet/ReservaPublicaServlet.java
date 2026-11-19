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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Alta de reserva sin exponer datos de otras personas. Para producción: rate-limit/CAPTCHA. */
@WebServlet("/api/reservas")
public final class ReservaPublicaServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JsonNode json = Api.body(req);
            JsonNode mesa = json.get("mesaId"), personas = json.get("personas"),
                     nombre = json.get("nombreContacto"), inicio = json.get("inicio"), fin = json.get("fin");
            if (mesa == null || !mesa.canConvertToLong() || mesa.longValue() <= 0 ||
                    personas == null || !personas.canConvertToInt() || personas.intValue() < 1 || personas.intValue() > 30 ||
                    nombre == null || !nombre.isTextual() || nombre.asText().trim().length() < 2 ||
                    nombre.asText().length() > 120 || inicio == null || !inicio.isTextual() ||
                    fin == null || !fin.isTextual())
                throw new Api.BadRequest(400, "Requiere mesaId, personas, nombreContacto, inicio y fin con zona horaria");
            OffsetDateTime desde, hasta;
            try { desde = OffsetDateTime.parse(inicio.asText()); hasta = OffsetDateTime.parse(fin.asText()); }
            catch (DateTimeParseException ex) { throw new Api.BadRequest(400, "Fechas ISO 8601 inválidas"); }
            if (!hasta.isAfter(desde) || !desde.isAfter(OffsetDateTime.now().minusMinutes(5)))
                throw new Api.BadRequest(400, "La reserva debe ser futura y tener un fin posterior");
            String telefono = json.hasNonNull("telefonoContacto") ? json.get("telefonoContacto").asText() : null;
            if (telefono != null && telefono.length() > 25)
                throw new Api.BadRequest(400, "Teléfono demasiado largo");
            long id;
            try (Connection conn = DatabasePool.conexion();
                 PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO mesacero.reservas(mesa_id,nombre_contacto,telefono_contacto,personas,inicio,fin) " +
                        "VALUES (?,?,?,?,?,?) RETURNING id")) {
                stmt.setLong(1, mesa.longValue()); stmt.setString(2, nombre.asText().trim());
                stmt.setString(3, telefono); stmt.setInt(4, personas.intValue());
                stmt.setObject(5, desde); stmt.setObject(6, hasta);
                try (ResultSet rs = stmt.executeQuery()) { rs.next(); id = rs.getLong(1); }
            }
            Api.send(res, 201, Map.of("id", id, "estado", "pendiente", "mensaje", "Reserva recibida"));
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
