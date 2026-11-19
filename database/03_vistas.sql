-- Vistas orientadas a Angular/Servlets, administracion y reportes.
BEGIN;
SET LOCAL search_path TO mesacero, public;

-- V01: carta publica; los archivos siguen servidos por Angular /assets/.
CREATE OR REPLACE VIEW vw_carta_publica AS
SELECT p.id, p.nombre, c.nombre AS categoria, p.precio, p.descripcion,
       p.foto_url AS foto, p.etiqueta, p.disponible, p.orden
FROM productos p JOIN categorias c ON c.id=p.categoria_id
WHERE p.activo=true AND c.activa=true AND p.disponible=true;

-- V02: estado operativo calculado. Una mesa fuera de servicio se devuelve
-- como 'ocupada' por compatibilidad con los tres estados del Angular actual.
CREATE OR REPLACE VIEW vw_estado_mesas AS
SELECT m.id, m.numero, m.capacidad, z.nombre AS zona,
       (m.estado_manual='fuera_servicio' OR NOT z.activa) AS fuera_servicio,
       CASE
         WHEN m.estado_manual='fuera_servicio' OR NOT z.activa THEN 'ocupada'
         WHEN EXISTS(SELECT 1 FROM pedidos p WHERE p.mesa_id=m.id
                     AND p.estado IN ('pendiente','en_preparacion','listo','entregado'))
           THEN 'ocupada'
         WHEN EXISTS(SELECT 1 FROM reservas r WHERE r.mesa_id=m.id
                     AND r.estado IN ('pendiente','confirmada')
                     AND r.inicio<=now() AND r.fin>now())
           THEN 'reservada'
         ELSE 'disponible'
       END AS estado
FROM mesas m JOIN zonas z ON z.id=m.zona_id;

-- V03: agenda de reservas, mesa, zona y titular.
CREATE OR REPLACE VIEW vw_reservas_detalle AS
SELECT r.id, r.usuario_id, r.mesa_id, m.numero AS numero_mesa,
       z.nombre AS zona, r.nombre_contacto, r.telefono_contacto,
       r.personas, r.inicio, r.fin, r.estado, r.notas, r.creado_en
FROM reservas r JOIN mesas m ON m.id=r.mesa_id
JOIN zonas z ON z.id=m.zona_id;

-- V04: importe efectivamente pagado, deuda y estatus de cada pedido.
CREATE OR REPLACE VIEW vw_cuenta_pedidos AS
SELECT p.id AS pedido_id, p.mesa_id, m.numero AS mesa_numero,
       p.usuario_id, p.mesero_id, p.estado, p.creado_en,
       p.subtotal, p.total, COALESCE(ab.pagado,0)::numeric(12,2) AS pagado,
       GREATEST(p.total-COALESCE(ab.pagado,0),0)::numeric(12,2) AS saldo,
       CASE WHEN p.total=0 THEN 'sin_cargo'
            WHEN COALESCE(ab.pagado,0)=0 THEN 'sin_pagar'
            WHEN COALESCE(ab.pagado,0)<p.total THEN 'parcial'
            ELSE 'pagado' END AS estado_pago
FROM pedidos p JOIN mesas m ON m.id=p.mesa_id
LEFT JOIN (
  SELECT pedido_id, sum(monto) AS pagado FROM pagos
  WHERE estado='confirmado' GROUP BY pedido_id
) ab ON ab.pedido_id=p.id;

-- V05: renglones de comandas con nombre y precio historicos.
CREATE OR REPLACE VIEW vw_pedidos_detalle AS
SELECT d.id AS detalle_id, d.pedido_id, p.mesa_id, m.numero AS mesa_numero,
       p.usuario_id, p.estado AS estado_pedido, p.creado_en,
       d.producto_id, d.producto_nombre, d.cantidad,
       d.precio_unitario, d.subtotal, d.observaciones
FROM pedido_detalles d JOIN pedidos p ON p.id=d.pedido_id
JOIN mesas m ON m.id=p.mesa_id;

-- V06: ingresos confirmados por dia local de Oaxaca / Ciudad de Mexico.
CREATE OR REPLACE VIEW vw_ventas_diarias AS
SELECT (p.creado_en AT TIME ZONE 'America/Mexico_City')::date AS fecha,
       count(*) AS numero_pagos,
       count(DISTINCT p.pedido_id) AS pedidos_cobrados,
       sum(p.monto)::numeric(14,2) AS ingresos_confirmados
FROM pagos p WHERE p.estado='confirmado'
GROUP BY (p.creado_en AT TIME ZONE 'America/Mexico_City')::date;

-- V07: grafica del dashboard de ventas por hora local.
CREATE OR REPLACE VIEW vw_ventas_por_hora AS
SELECT (p.creado_en AT TIME ZONE 'America/Mexico_City')::date AS fecha,
       extract(hour FROM p.creado_en AT TIME ZONE 'America/Mexico_City')::int AS hora,
       count(*) AS pagos,
       sum(p.monto)::numeric(14,2) AS ingresos_confirmados
FROM pagos p WHERE p.estado='confirmado'
GROUP BY (p.creado_en AT TIME ZONE 'America/Mexico_City')::date,
         extract(hour FROM p.creado_en AT TIME ZONE 'America/Mexico_City')::int;

-- V08: platos mas vendidos; solo pedidos completamente cobrados.
CREATE OR REPLACE VIEW vw_productos_mas_vendidos AS
WITH pedidos_pagados AS (
  SELECT pedido_id FROM vw_cuenta_pedidos WHERE estado_pago='pagado'
)
SELECT d.producto_id, d.producto_nombre,
       sum(d.cantidad)::bigint AS unidades_vendidas,
       sum(d.subtotal)::numeric(14,2) AS ingreso_por_producto
FROM pedido_detalles d JOIN pedidos_pagados pp ON pp.pedido_id=d.pedido_id
GROUP BY d.producto_id,d.producto_nombre;

-- V09: corte de caja; distingue efectivo esperado y contado.
CREATE OR REPLACE VIEW vw_cortes_caja AS
SELECT t.id AS turno_id, t.cajero_id, u.nombre AS cajero,
       t.abierto_en, t.cerrado_en, t.estado,
       t.apertura_efectivo, COALESCE(p.ingresos_efectivo,0)::numeric(12,2) AS ingresos_efectivo,
       (t.apertura_efectivo+COALESCE(p.ingresos_efectivo,0))::numeric(12,2) AS efectivo_esperado,
       t.cierre_declarado,
       CASE WHEN t.cierre_declarado IS NULL THEN NULL
            ELSE (t.cierre_declarado-t.apertura_efectivo-COALESCE(p.ingresos_efectivo,0))::numeric(12,2)
       END AS diferencia
FROM turnos_caja t JOIN usuarios u ON u.id=t.cajero_id
LEFT JOIN (
  SELECT turno_caja_id,sum(monto) AS ingresos_efectivo
  FROM pagos WHERE estado='confirmado' AND metodo='efectivo'
  GROUP BY turno_caja_id
) p ON p.turno_caja_id=t.id;

-- V10: indicadores resumen del panel de administrador.
CREATE OR REPLACE VIEW vw_dashboard_resumen AS
SELECT
 (SELECT count(*) FROM mesas) AS mesas_totales,
 (SELECT count(*) FROM vw_estado_mesas WHERE estado='disponible') AS mesas_libres,
 (SELECT count(*) FROM vw_estado_mesas WHERE estado='ocupada') AS mesas_ocupadas,
 (SELECT count(*) FROM vw_estado_mesas WHERE estado='reservada') AS mesas_reservadas,
 (SELECT count(*) FROM productos WHERE activo AND disponible) AS productos_publicados,
 (SELECT count(*) FROM reservas WHERE estado IN ('pendiente','confirmada')) AS reservas_activas,
 (SELECT count(*) FROM pedidos WHERE estado <> 'cancelado') AS pedidos_registrados,
 (SELECT COALESCE(sum(monto),0) FROM pagos WHERE estado='confirmado')::numeric(14,2)
   AS ventas_confirmadas;

COMMIT;
