package mx.mesacero.dao;

import java.sql.SQLException;
import java.util.List;

/** Operaciones CRUD que implementa cada DAO. */
public interface CrudDAO<T> {
    T crear(T objeto) throws SQLException;
    T buscarPorId(int id) throws SQLException;
    List<T> listar() throws SQLException;
    boolean actualizar(T objeto) throws SQLException;
    boolean eliminar(int id) throws SQLException;
}
