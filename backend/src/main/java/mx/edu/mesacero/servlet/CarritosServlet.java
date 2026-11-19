package mx.edu.mesacero.servlet;
import jakarta.servlet.annotation.WebServlet;
import mx.edu.mesacero.crud.CrudServlet;
import mx.edu.mesacero.crud.TableSpec;
@WebServlet("/api/admin/carritos/*")
public final class CarritosServlet extends CrudServlet { @Override protected TableSpec spec() { return TableSpec.CARRITOS; } }
