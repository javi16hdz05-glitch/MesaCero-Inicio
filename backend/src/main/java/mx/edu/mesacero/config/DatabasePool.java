package mx.edu.mesacero.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Único pool compartido por los Servlets; se cierra al apagar la aplicación. */
public final class DatabasePool {
    private static volatile HikariDataSource pool;

    private DatabasePool() {}

    public static synchronized void iniciar() {
        if (pool != null) return;
        String secret = System.getenv("DB_PASSWORD");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Configura DB_PASSWORD en el entorno del servidor");
        }
        HikariConfig c = new HikariConfig();
        c.setPoolName("MesaCeroPool");
        c.setJdbcUrl(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/mesa_cero"));
        c.setUsername(System.getenv().getOrDefault("DB_USER", "postgres"));
        c.setPassword(secret);
        c.setMaximumPoolSize(10);
        c.setMinimumIdle(2);
        c.setConnectionTimeout(30_000);
        c.setIdleTimeout(600_000);
        c.setMaxLifetime(1_800_000);
        c.setConnectionInitSql("SET search_path TO mesacero, public");
        pool = new HikariDataSource(c);
    }

    public static Connection conexion() throws SQLException {
        if (pool == null) throw new SQLException("Pool no iniciado");
        return pool.getConnection();
    }

    public static synchronized void cerrar() {
        if (pool != null) { pool.close(); pool = null; }
    }
}
