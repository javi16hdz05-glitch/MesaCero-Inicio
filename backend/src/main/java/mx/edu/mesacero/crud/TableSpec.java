package mx.edu.mesacero.crud;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Solo identificadores escritos en Java, nunca elegidos por la entrada del navegador. */
public final class TableSpec {
    public final String table;
    public final Map<String, String> writable;

    private TableSpec(String table, Map<String, String> writable) {
        this.table = "mesacero." + table;
        this.writable = Collections.unmodifiableMap(writable);
    }

    /** Tipos: text, int, long, decimal, bool, timestamp. */
    private static TableSpec define(String table, String... entries) {
        Map<String, String> cols = new LinkedHashMap<>();
        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            cols.put(parts[0], parts[1]);
        }
        return new TableSpec(table, cols);
    }

    public static final TableSpec CATEGORIAS = define("categorias", "nombre:text", "orden:int", "activa:bool");
    public static final TableSpec ZONAS = define("zonas", "nombre:text", "descripcion:text", "activa:bool");
    public static final TableSpec MESAS = define("mesas", "zona_id:int", "numero:int", "capacidad:int", "estado_manual:text");
    public static final TableSpec PRODUCTOS = define("productos", "categoria_id:int", "nombre:text", "descripcion:text",
            "precio:decimal", "foto_url:text", "etiqueta:text", "disponible:bool", "activo:bool", "orden:int");
    public static final TableSpec RESERVAS = define("reservas", "usuario_id:long", "mesa_id:long",
            "nombre_contacto:text", "telefono_contacto:text", "personas:int", "inicio:timestamp",
            "fin:timestamp", "estado:text", "notas:text");
    public static final TableSpec PEDIDOS = define("pedidos", "mesa_id:long", "usuario_id:long", "mesero_id:long",
            "reserva_id:long", "estado:text", "notas:text");
    public static final TableSpec DETALLES = define("pedido_detalles", "pedido_id:long", "producto_id:long",
            "cantidad:int", "observaciones:text");
    public static final TableSpec CARRITOS = define("carritos", "usuario_id:long", "sesion_anonima_hash:text", "estado:text");
    public static final TableSpec TURNOS = define("turnos_caja", "cajero_id:long", "apertura_efectivo:decimal",
            "cierre_declarado:decimal", "cerrado_en:timestamp", "estado:text");
    public static final TableSpec ROLES = define("roles", "nombre:text");
}
