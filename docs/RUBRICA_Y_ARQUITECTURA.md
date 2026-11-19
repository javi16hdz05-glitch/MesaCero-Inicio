# Mesa Cero | Servlets, HTTP, CRUD, HikariCP, Jackson y Postgres

## Qué se lee en las capturas de clase

Se distinguen `pom.xml` de Maven, pool `HikariCP`, driver PostgreSQL `org.postgresql.Driver`, `ConexionDB` con `HikariConfig` y `HikariDataSource`, `ServletContextListener` para abrir/cerrar conexiones, `@WebListener`, métodos `doGet` y ejemplos de `PATCH`, SQL con `PreparedStatement`, `try-with-resources`, filtros CORS, Jackson, DTO y POJO. Las fotos **no muestran una rúbrica formal** que permita confirmar la cantidad exacta de métodos por tabla.

Los métodos HTTP:

| Método | Operación CRUD | Ejemplo |
| --- | --- | --- |
| GET | Read (consultar) | Obtener una mesa o la carta |
| POST | Create (crear) | Crear un platillo, reserva o comanda |
| PUT | Update (reemplazo completo) | Reemplazar todos los campos editables de un registro |
| PATCH | Update parcial | Cambiar solo disponibilidad de un platillo |
| DELETE | Delete (eliminar) | Eliminar una categoría sin relaciones |

**No existe PUSH como método REST habitual**. `git push` es distinto: subir commits a GitHub.

## Correspondencia de tablas y Servlets

Contexto del WAR: `/mesacero-api`. Todas las rutas de la tabla llevan prefijo `/api/`.

| Tabla PostgreSQL | Servlet | Ruta | Métodos | Observaciones |
| --- | --- | --- | --- | --- |
| `categorias` | `CategoriasServlet` | `admin/categorias` | GET POST PUT PATCH DELETE | CRUD protegido |
| `zonas` | `ZonasServlet` | `admin/zonas` | GET POST PUT PATCH DELETE | CRUD protegido |
| `mesas` | `MesasServlet` | `admin/mesas` | GET POST PUT PATCH DELETE | Estado operativo calculado por vista |
| `productos` | `ProductosServlet` | `admin/productos` | GET POST PUT PATCH DELETE | No admite modificar el precio histórico de un detalle |
| `reservas` | `ReservasServlet` | `admin/reservas` | GET POST PUT PATCH DELETE | Validaciones/solapamiento en BD |
| `pedidos` | `PedidosServlet` | `admin/pedidos` | GET POST PUT PATCH DELETE | Totales no editables desde API |
| `pedido_detalles` | `PedidoDetallesServlet` | `admin/pedido-detalles` | GET POST PUT PATCH DELETE | Trigger fija precio y total |
| `carritos` | `CarritosServlet` | `admin/carritos` | GET POST PUT PATCH DELETE | Demo: restringido por llave administrativa |
| `carrito_items` | `CarritoItemsServlet` | `admin/carrito-items` | GET POST PUT PATCH DELETE | Llave compuesta `/carritoId/productoId` |
| `turnos_caja` | `TurnosCajaServlet` | `admin/turnos-caja` | GET POST PUT PATCH DELETE | Cierre sujeto a reglas de BD |
| `roles` | `RolesServlet` | `admin/roles` | GET POST PUT PATCH DELETE | En producción debería ser catálogo inmutable |
| `pagos` | `PagosServlet` | `admin/pagos` | GET POST | Sin DELETE ni PUT públicos para no borrar o falsificar ingresos |
| `usuarios` | — | — | — | Requiere registro, login, contraseñas hasheadas y roles, aún pendiente |
| `tokens_recuperacion` | — | — | — | Interno de seguridad; no exponer CRUD |
| `pedido_historial` | `ReportesServlet` (indirecto) | — | — | Generado por triggers; no debe poder modificarse manualmente |
| `auditoria` | — | — | — | Generada por triggers; no debe poder modificarse manualmente |

**Total: 18 Servlets**. Los 11 CRUD cumplen más de diez controladores y los cinco métodos en esas 11 tablas. Si la maestra exige los cuatro verbos **en las 16 tablas**, hay que pedirle confirmación de cómo espera tratar historiales, tokens y pagos: habilitar `DELETE` de auditoría o tokens sería una mala práctica real.

Otros Servlets:

| Servlet | Ruta | Métodos | Función |
| --- | --- | --- | --- |
| `CatalogoServlet` | `catalogo` | GET | Vista `vw_carta_publica`, forma de `Producto` Angular |
| `EstadoMesasServlet` | `estado-mesas` | GET | Vista `vw_estado_mesas` |
| `HealthServlet` | `health` | GET | Confirmar conexión PostgreSQL |
| `ReservaPublicaServlet` | `reservas` | POST | Inserción de reserva sin mostrar PII de terceros |
| `ComandasServlet` | `admin/comandas` | POST | Transacción atómica de pedido + renglones |
| `ReportesServlet` | `admin/reportes?vista=...` | GET | Ocho vistas analíticas permitidas |

## Diseño por capas

```text
Angular (vistas, cascada, carrito de demostración)
  ↓ HttpClient / proxy local
Servlets: HTTP GET/POST/PUT/PATCH/DELETE → Jackson JSON
  ↓ Filtro CORS + llave de DEMO para /admin/*
JDBC PreparedStatement + HikariCP (10 conexiones máximo)
  ↓
PostgreSQL mesa_cero, esquema mesacero
  ↓
16 tablas + 10 vistas + 22 triggers (del paquete SQL original)
```

`CrudServlet` implementa los métodos reutilizables y cada clase concreta selecciona una `TableSpec`. **Cada tabla cubierta posee su propia clase Java Servlet** aunque compartan lógica de JDBC. `ProductoDto` y `MesaEstadoDto` son objetos Java tipo POJO/DTO que seleccionan las propiedades públicas enviadas a Angular. Las columnas de SQL están en una lista blanca estática para evitar inyección por nombres de columnas; los valores se pasan con `PreparedStatement`.

### Códigos de respuesta

- `200` consulta/actualización exitosa; `201` creación; `204` eliminación.
- `400` datos inválidos; `401` falta llave administrativa; `404` registro no existente.
- `409` conflicto por clave única, relación o reserva solapada; `415` body no JSON; `500` SQL inesperado; `503` configuración o BD no disponible.

### Responsabilidades de las vistas y triggers

Las vistas `vw_carta_publica` y `vw_estado_mesas` hacen las lecturas iniciales de Angular. `vw_dashboard_resumen`, `vw_cuenta_pedidos`, `vw_ventas_diarias`, `vw_ventas_por_hora`, `vw_productos_mas_vendidos`, `vw_cortes_caja`, `vw_reservas_detalle` y `vw_pedidos_detalle` exponen consultas operativas. Consulta el PDF `MesaCero_Vistas_Triggers.pdf` del paquete PostgreSQL para el detalle de las 22 reglas automáticas.

- Un pedido se crea en `pedidos` y sus productos en `pedido_detalles` en **una sola transacción**, así no se guardan comandas a medias.
- Los precios, nombres históricos y subtotales de detalles quedan fijados por triggers de la base; nunca se confía en el precio enviado desde Angular.
- Al registrar un pago, la base comprueba el monto y evita superar el total; el efectivo requiere un turno de caja abierto.
- Las reservas tienen restricción temporal sin traslapes y validación de capacidad.
- Los cambios relevantes generan histórico y auditoría.

## Cómo probar con Postman o Insomnia

1. Levanta PostgreSQL, despliega el WAR, revisa `GET /mesacero-api/api/health`.
2. Revisa `GET /mesacero-api/api/catalogo` y confirma las 7 imágenes en `frontend/public/assets/`.
3. Agrega una categoría de PRUEBA con `POST /api/admin/categorias`, header `X-Admin-Key: <tu variable>` y cuerpo JSON `{"nombre":"Especiales","orden":8,"activa":true}`.
4. Haz `GET`, `PATCH {"orden":9}`, `PUT {"nombre":"Especiales","orden":9,"activa":true}` y `DELETE` con el ID recibido.
5. Haz `POST /api/admin/comandas` con `{"mesaId":1,"items":[{"productoId":1,"cantidad":2}]}`. Consulta el pedido y comprueba que total se calculó desde el precio de BD.
6. Intenta repetir la reserva en la misma mesa/hora; la segunda debe recibir `409`, siempre que la BD tenga el trigger y la restricción de exclusión.

## Lo que falta antes de una entrega final completa

- Confirmar la **rúbrica literal**: número de Servlets y si piden métodos por tabla, controlador o sistema.
- Implementar login real (roles cliente/mesero/administrador/cajero), hash seguro de contraseñas, sesiones, permisos por recurso y propietario de reservas/pedidos.
- Conectar los botones actuales de Angular con HTTP. El frontend original todavía es demo en memoria; el servicio TypeScript incluido es un primer puente.
- Pruebas automatizadas de endpoints y transacciones con una instancia de PostgreSQL real.
- Endurecer seguridad: HTTPS, anti-abuso para reservas, CSRF si se utilizan cookies, validación completa, rotación de secretos y separación de privilegios.
- Confirmar que Maven/WAR compila y se despliega en la computadora del equipo.
