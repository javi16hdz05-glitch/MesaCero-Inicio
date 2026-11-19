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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** Cobros: solo consulta y alta; cancelar/reembolsar necesita flujo autorizado propio. */
@WebServlet("/api/admin/pagos")
public final class PagosServlet extends HttpServlet {
    private static final Set<String> METODOS = Set.of("efectivo", "tarjeta", "transferencia");
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var rows = new ArrayList<Map<String, Object>>();
        try (Connection conn = DatabasePool.conexion();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id,pedido_id,turno_caja_id,metodo,estado,monto,creado_en FROM mesacero.pagos ORDER BY id DESC LIMIT 100");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) rows.add(Api.row(rs));
            Api.send(res, 200, rows);
        } catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            JsonNode body = Api.body(req);
            JsonNode pedido = body.get("pedidoId"), monto = body.get("monto");
            JsonNode metodo = body.get("metodo"), turno = body.get("turnoCajaId");
            if (pedido == null || !pedido.canConvertToLong() || pedido.longValue() <= 0 ||
                    monto == null || !monto.isNumber() || monto.decimalValue().signum() <= 0 ||
                    metodo == null || !metodo.isTextual() || !METODOS.contains(metodo.asText()) ||
                    ("efectivo".equals(metodo.asText()) && (turno == null || !turno.canConvertToLong())))
                throw new Api.BadRequest(400, "Requiere pedidoId, monto positivo, metodo y turnoCajaId para efectivo");
            try (Connection conn = DatabasePool.conexion();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO mesacero.pagos(pedido_id,turno_caja_id,metodo,estado,monto) " +
                         "VALUES (?,?,?,'confirmado',?) RETURNING id,pedido_id,turno_caja_id,metodo,estado,monto,creado_en")) {
                stmt.setLong(1, pedido.longValue());
                if (turno == null || turno.isNull()) stmt.setObject(2, null);
                else if (turno.canConvertToLong() && turno.longValue() > 0) stmt.setLong(2, turno.longValue());
                else throw new Api.BadRequest(400, "turnoCajaId inválido");
                stmt.setString(3, metodo.asText());
                stmt.setBigDecimal(4, new BigDecimal(monto.asText()));
                try (ResultSet rs = stmt.executeQuery()) { rs.next(); Api.send(res, 201, Api.row(rs)); }
            }
        } catch (Api.BadRequest ex) { Api.error(res, ex.status, ex.getMessage()); }
        catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
