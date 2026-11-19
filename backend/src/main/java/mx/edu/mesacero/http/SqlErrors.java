package mx.edu.mesacero.http;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SqlErrors {
    private static final Logger LOG = Logger.getLogger(SqlErrors.class.getName());
    private SqlErrors() {}

    public static void handle(HttpServletResponse res, SQLException ex) throws IOException {
        String state = ex.getSQLState();
        if ("23505".equals(state) || "23P01".equals(state)) {
            Api.error(res, 409, "Conflicto: ya existe ese registro o hay una reserva superpuesta");
        } else if ("23503".equals(state)) {
            Api.error(res, 409, "Conflicto con un registro relacionado (clave externa)");
        } else if (state != null && (state.startsWith("22") || state.equals("23502") || state.equals("23514")
                || state.equals("P0001"))) {
            Api.error(res, 400, "Datos o estado de negocio inválidos; verifica la petición");
        } else {
            LOG.log(Level.SEVERE, "Fallo de SQL", ex);
            Api.error(res, 500, "Error interno de base de datos");
        }
    }
}
