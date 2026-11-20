package mx.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.mesacero.dao.MesaDAO;
import mx.mesacero.dao.ReservaDAO;
import mx.mesacero.model.Reserva;

import java.io.IOException;

/** /api/reservas  y  /api/reservas/{id} */
@WebServlet("/api/reservas/*")
public class ReservaServlet extends BaseServlet {

    private final ReservaDAO dao = new ReservaDAO();
    private final MesaDAO mesaDAO = new MesaDAO();

    private void validar(Reserva r) {
        exigir(r.getMesaId() > 0, "mesaId es obligatorio");
        exigir(r.getFecha() != null && r.getFecha().matches("\\d{4}-\\d{2}-\\d{2}"),
                "La fecha debe tener formato yyyy-MM-dd");
        exigir(r.getHora() != null && r.getHora().matches("\\d{2}:\\d{2}(:\\d{2})?"),
                "La hora debe tener formato HH:mm");
        exigir(r.getPersonas() > 0, "El número de personas debe ser mayor a 0");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = obtenerId(req);
            if (id == null) {
                enviarJson(resp, 200, dao.listar());
                return;
            }
            Reserva r = dao.buscarPorId(id);
            if (r == null) {
                enviarError(resp, 404, "Reservación no encontrada");
            } else {
                enviarJson(resp, 200, r);
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Reserva r = leerCuerpo(req, Reserva.class);
            validar(r);
            dao.crear(r);
            mesaDAO.actualizarEstado(r.getMesaId(), "reservada");
            enviarJson(resp, 201, r);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Reserva r = leerCuerpo(req, Reserva.class);
            r.setId(id);
            validar(r);
            if (dao.actualizar(r)) {
                enviarJson(resp, 200, r);
            } else {
                enviarError(resp, 404, "Reservación no encontrada");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Reserva existente = dao.buscarPorId(id);
            if (existente == null) {
                enviarError(resp, 404, "Reservación no encontrada");
                return;
            }
            Reserva r = aplicarParche(existente, req, Reserva.class);
            validar(r);
            dao.actualizar(r);
            enviarJson(resp, 200, r);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** Al cancelar la reservación, la mesa vuelve a quedar disponible. */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Reserva r = dao.buscarPorId(id);
            if (r == null || !dao.eliminar(id)) {
                enviarError(resp, 404, "Reservación no encontrada");
                return;
            }
            mesaDAO.actualizarEstado(r.getMesaId(), "disponible");
            resp.setStatus(204);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
