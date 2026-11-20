package mx.mesacero.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Clase base de los servlets. Aquí van las cosas que todos usan:
 * lectura/escritura de JSON, manejo de errores y soporte para PATCH
 * (HttpServlet no trae doPatch, así que se agrega interceptando service()).
 */
public abstract class BaseServlet extends HttpServlet {

    protected static final Gson GSON = new Gson();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /** Cada servlet sobrescribe este método. Si no lo hace, responde 405. */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        enviarError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "PATCH no soportado en este recurso");
    }

    // ---------- Utilidades ----------

    /** Devuelve el id de /api/recurso/{id}, o null si la ruta es /api/recurso. */
    protected Integer obtenerId(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) {
            return null;
        }
        return Integer.parseInt(info.substring(1));
    }

    protected Integer idObligatorio(HttpServletRequest req) {
        Integer id = obtenerId(req);
        if (id == null) {
            throw new IllegalArgumentException("Falta el id en la ruta (ej. /api/recurso/1)");
        }
        return id;
    }

    protected <T> T leerCuerpo(HttpServletRequest req, Class<T> clase) throws IOException {
        T objeto = GSON.fromJson(req.getReader(), clase);
        if (objeto == null) {
            throw new IllegalArgumentException("El cuerpo de la petición está vacío");
        }
        return objeto;
    }

    /** Mezcla los campos enviados en el PATCH con el objeto que ya existe. */
    protected <T> T aplicarParche(T existente, HttpServletRequest req, Class<T> clase) throws IOException {
        JsonElement elemento = JsonParser.parseReader(req.getReader());
        if (!elemento.isJsonObject()) {
            throw new IllegalArgumentException("El cuerpo debe ser un objeto JSON");
        }
        JsonObject parche = elemento.getAsJsonObject();
        parche.remove("id"); // el id no se puede cambiar
        JsonObject base = GSON.toJsonTree(existente).getAsJsonObject();
        for (Map.Entry<String, JsonElement> campo : parche.entrySet()) {
            base.add(campo.getKey(), campo.getValue());
        }
        return GSON.fromJson(base, clase);
    }

    protected void enviarJson(HttpServletResponse resp, int estado, Object cuerpo) throws IOException {
        resp.setStatus(estado);
        resp.getWriter().write(GSON.toJson(cuerpo));
    }

    protected void enviarError(HttpServletResponse resp, int estado, String mensaje) throws IOException {
        enviarJson(resp, estado, Map.of("error", mensaje));
    }

    /** Traduce las excepciones más comunes a códigos HTTP. */
    protected void manejarError(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof NumberFormatException) {
            enviarError(resp, 400, "El id debe ser un número");
        } else if (e instanceof IllegalArgumentException || e instanceof JsonParseException) {
            enviarError(resp, 400, e.getMessage());
        } else if (e instanceof SQLException) {
            String estadoSql = ((SQLException) e).getSQLState();
            if ("23505".equals(estadoSql)) {
                enviarError(resp, 409, "Ya existe un registro con esos datos");
            } else if ("23503".equals(estadoSql)) {
                enviarError(resp, 409, "El registro está relacionado con otros o hace referencia a uno que no existe");
            } else if ("23514".equals(estadoSql)) {
                enviarError(resp, 400, "Algún valor no cumple las restricciones de la base de datos");
            } else {
                log("Error de base de datos", e);
                enviarError(resp, 500, "Error en la base de datos");
            }
        } else {
            log("Error inesperado", e);
            enviarError(resp, 500, "Error interno del servidor");
        }
    }

    protected void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
