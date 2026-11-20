package mx.mesacero.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión JDBC a PostgreSQL.
 * Los datos se leen de variables de entorno para no subir contraseñas al repo:
 * DB_URL, DB_USER y DB_PASSWORD.
 */
public final class ConexionBD {

    private static final String URL = leer("DB_URL", "jdbc:postgresql://localhost:5432/mesacero");
    private static final String USUARIO = leer("DB_USER", "postgres");
    private static final String CLAVE = leer("DB_PASSWORD", "postgres");

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ConexionBD() { }

    private static String leer(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
