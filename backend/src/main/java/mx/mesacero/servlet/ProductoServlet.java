package mx.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.mesacero.dao.ProductoDAO;
import mx.mesacero.model.Producto;

import java.io.IOException;
import java.util.Set;

/** /api/productos  y  /api/productos/{id} */
@WebServlet("/api/productos/*")
public class ProductoServlet extends BaseServlet {

    private static final Set<String> CATEGORIAS = Set.of("Oaxaqueños", "Fuertes", "Bebidas", "Postres");
    private final ProductoDAO dao = new ProductoDAO();

    private void validar(Producto p) {
        exigir(p.getNombre() != null && !p.getNombre().isBlank(), "El nombre es obligatorio");
        exigir(p.getCategoria() != null && CATEGORIAS.contains(p.getCategoria()),
                "Categoría inválida (Oaxaqueños, Fuertes, Bebidas o Postres)");
        exigir(p.getPrecio() > 0, "El precio debe ser mayor a 0");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = obtenerId(req);
            if (id == null) {
                enviarJson(resp, 200, dao.listar());
                return;
            }
            Producto p = dao.buscarPorId(id);
            if (p == null) {
                enviarError(resp, 404, "Producto no encontrado");
            } else {
                enviarJson(resp, 200, p);
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Producto p = leerCuerpo(req, Producto.class);
            validar(p);
            enviarJson(resp, 201, dao.crear(p));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Producto p = leerCuerpo(req, Producto.class);
            p.setId(id);
            validar(p);
            if (dao.actualizar(p)) {
                enviarJson(resp, 200, p);
            } else {
                enviarError(resp, 404, "Producto no encontrado");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** Ejemplo: {"disponible": false} para marcar un platillo como agotado. */
    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = idObligatorio(req);
            Producto existente = dao.buscarPorId(id);
            if (existente == null) {
                enviarError(resp, 404, "Producto no encontrado");
                return;
            }
            Producto p = aplicarParche(existente, req, Producto.class);
            validar(p);
            dao.actualizar(p);
            enviarJson(resp, 200, p);
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
                enviarError(resp, 404, "Producto no encontrado");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
