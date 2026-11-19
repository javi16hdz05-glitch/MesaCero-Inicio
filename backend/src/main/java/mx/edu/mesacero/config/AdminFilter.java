package mx.edu.mesacero.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Llave de DEMOSTRACIÓN; sustituir por autenticación con roles antes de publicar. */
@WebFilter(urlPatterns = "/api/admin/*")
public class AdminFilter implements Filter {
    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if ("OPTIONS".equals(request.getMethod())) { chain.doFilter(req, res); return; }
        String expected = System.getenv("API_ADMIN_KEY");
        String supplied = request.getHeader("X-Admin-Key");
        if (expected == null || expected.length() < 24) {
            response.sendError(503, "Falta configurar API_ADMIN_KEY (mínimo 24 caracteres)");
            return;
        }
        if (supplied == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(401, "Acceso restringido");
            return;
        }
        chain.doFilter(req, res);
    }
}
