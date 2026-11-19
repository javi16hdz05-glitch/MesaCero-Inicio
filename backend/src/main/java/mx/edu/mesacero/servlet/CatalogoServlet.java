package mx.edu.mesacero.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.mesacero.config.DatabasePool;
import mx.edu.mesacero.dto.ProductoDto;
import mx.edu.mesacero.http.Api;
import mx.edu.mesacero.http.SqlErrors;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/** DTO compatible con Producto del Angular original. No filtra contraseñas ni datos personales. */
@WebServlet("/api/catalogo")
public final class CatalogoServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var productos = new ArrayList<ProductoDto>();
        try (Connection conn = DatabasePool.conexion();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id,nombre,categoria,precio,disponible,descripcion,foto,etiqueta " +
                "FROM mesacero.vw_carta_publica ORDER BY orden,id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) productos.add(new ProductoDto(
                    rs.getLong("id"), rs.getString("nombre"), rs.getString("categoria"),
                    rs.getBigDecimal("precio"), rs.getBoolean("disponible"),
                    rs.getString("descripcion"), rs.getString("foto"), rs.getString("etiqueta")));
            Api.send(res, 200, productos);
        } catch (SQLException ex) { SqlErrors.handle(res, ex); }
    }
}
