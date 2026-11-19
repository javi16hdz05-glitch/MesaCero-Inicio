package mx.edu.mesacero.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        DatabasePool.iniciar();
        event.getServletContext().log("MesaCero: pool PostgreSQL inicializado");
    }

    @Override public void contextDestroyed(ServletContextEvent event) {
        DatabasePool.cerrar();
    }
}
