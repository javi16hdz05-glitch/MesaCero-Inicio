package mx.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.mesacero.dao.ComandaDAO;
import mx.mesacero.model.Comanda;
import mx.mesacero.model.ItemComanda;

import java.io.IOException;
import java.util.Set;

/** /api/comandas  y  /api/comandas/{id} */
@WebServlet("/api/comandas/*")
public class ComandaServlet extends BaseServlet {

    private static final Set<String> ESTADOS = Set.of("pendiente", "en_preparacion", "completada");
    private final ComandaDAO dao = new ComandaDAO();

    private void validar(Comanda c) {
        exigir(c.getMesaId() > 0, "mesaId es obligatorio");
        exigir(c.getEstado() != null && ESTADOS.contains(c.getEstado()),
                "Estado inválido (pendiente, en_preparacion o completada)");
        exigir(c.getItems() != null && !c.getItems().isEmpty(), "La comanda necesita al menos un producto");
        for (ItemComanda i : c.getItems()) {
            exigir(i.getProductoId() > 0, "productoId inválido");
            exigir(i.getCantidad() > 0, "La cantidad debe ser mayor a 0");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = obtenerId(req);
            if (id == null) {
                enviarJson(resp, 200, dao.listar());
                return;
            }
            Comanda c = dao.buscarPorId(id);
            if (c == null) {
                enviarError(resp, 404, "Comanda no encontrada");
            } else {
                enviarJson(resp, 200, c);
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** Registra una comanda nueva; el total lo calcula el servidor y la mesa pasa a ocupada. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Comanda c = leerCuerpo(req, Comanda.class);
            c.setEstado("en_preparacion");
            validar(c);
            enviarJson(resp, 201, dao.crear(c));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Comanda c = leerCuerpo(req, Comanda.class);
            c.setId(id);
            validar(c);
            if (dao.actualizar(c)) {
                enviarJson(resp, 200, dao.buscarPorId(id));
            } else {
                enviarError(resp, 404, "Comanda no encontrada");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** Ejemplo: {"estado":"completada"} cuando cocina termina el pedido. */
    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Comanda existente = dao.buscarPorId(id);
            if (existente == null) {
                enviarError(resp, 404, "Comanda no encontrada");
                return;
            }
            Comanda c = aplicarParche(existente, req, Comanda.class);
            validar(c);
            dao.actualizar(c);
            enviarJson(resp, 200, dao.buscarPorId(id));
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
                enviarError(resp, 404, "Comanda no encontrada");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
