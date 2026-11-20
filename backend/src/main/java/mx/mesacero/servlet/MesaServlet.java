package mx.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.mesacero.dao.MesaDAO;
import mx.mesacero.model.Mesa;

import java.io.IOException;
import java.util.Set;

/** /api/mesas  y  /api/mesas/{id} */
@WebServlet("/api/mesas/*")
public class MesaServlet extends BaseServlet {

    private static final Set<String> ESTADOS = Set.of("disponible", "ocupada", "reservada");
    private final MesaDAO dao = new MesaDAO();

    private void validar(Mesa m) {
        exigir(m.getNumero() > 0, "El número de mesa debe ser mayor a 0");
        exigir(m.getCapacidad() > 0, "La capacidad debe ser mayor a 0");
        exigir(m.getEstado() != null && ESTADOS.contains(m.getEstado()),
                "Estado inválido (disponible, ocupada o reservada)");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = obtenerId(req);
            if (id == null) {
                enviarJson(resp, 200, dao.listar());
                return;
            }
            Mesa m = dao.buscarPorId(id);
            if (m == null) {
                enviarError(resp, 404, "Mesa no encontrada");
            } else {
                enviarJson(resp, 200, m);
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Mesa m = leerCuerpo(req, Mesa.class);
            if (m.getEstado() == null) {
                m.setEstado("disponible");
            }
            validar(m);
            enviarJson(resp, 201, dao.crear(m));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** PUT reemplaza la mesa completa. */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Mesa m = leerCuerpo(req, Mesa.class);
            m.setId(id);
            validar(m);
            if (dao.actualizar(m)) {
                enviarJson(resp, 200, m);
            } else {
                enviarError(resp, 404, "Mesa no encontrada");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** PATCH cambia solo los campos enviados (ej. {"estado":"ocupada"}). */
    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Mesa existente = dao.buscarPorId(id);
            if (existente == null) {
                enviarError(resp, 404, "Mesa no encontrada");
                return;
            }
            Mesa m = aplicarParche(existente, req, Mesa.class);
            validar(m);
            dao.actualizar(m);
            enviarJson(resp, 200, m);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (dao.eliminar(idObligatorio(req))) {
                resp.setStatus(204);
            } else {
                enviarError(resp, 404, "Mesa no encontrada");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
