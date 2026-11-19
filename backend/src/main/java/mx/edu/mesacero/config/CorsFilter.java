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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** No usa '*' para permitir solo orígenes especificados por el equipo. */
@WebFilter(urlPatterns = "/api/*")
public class CorsFilter implements Filter {
    private final Set<String> origins = Arrays.stream(System.getenv()
            .getOrDefault("CORS_ORIGINS", "http://localhost:4200").split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());

    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String origin = request.getHeader("Origin");
        response.addHeader("Vary", "Origin");
        if (origin != null && origins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Admin-Key");
        }
        if ("OPTIONS".equals(request.getMethod())) {
            if (origin != null && !origins.contains(origin)) {
                response.sendError(403);
            } else {
                response.setStatus(204);
            }
            return;
        }
        chain.doFilter(req, res);
    }
}
