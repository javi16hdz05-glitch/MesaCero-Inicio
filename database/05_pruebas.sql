-- Pruebas transaccionales: no dejan datos persistentes (ROLLBACK final).
-- Ejecutar despues de los cuatro scripts de instalacion.
BEGIN;
SET LOCAL search_path TO mesacero, public;

DO $$
DECLARE v_mesa bigint; v_prod bigint; v_pedido bigint; v_precio numeric(12,2);
        v_total numeric(12,2); v_pagado numeric(12,2); v_reserva bigint;
        v_inicio timestamptz;
BEGIN
  SELECT id INTO v_mesa FROM mesas WHERE numero=1;
  SELECT id,precio INTO v_prod,v_precio FROM productos WHERE nombre='Tlayuda especial';
  IF v_mesa IS NULL OR v_prod IS NULL THEN
    RAISE EXCEPTION 'Falta cargar los datos de prueba';
  END IF;

  INSERT INTO pedidos(mesa_id) VALUES(v_mesa) RETURNING id INTO v_pedido;
  -- El trigger debe IGNORAR los precios que envie el cliente.
  INSERT INTO pedido_detalles(pedido_id,producto_id,producto_nombre,cantidad,precio_unitario)
    VALUES(v_pedido,v_prod,'Nombre falso',2,1);
  SELECT total INTO v_total FROM pedidos WHERE id=v_pedido;
  IF v_total<>v_precio*2 THEN RAISE EXCEPTION 'Fallo recalculo de totales'; END IF;

  UPDATE productos SET precio=precio+10 WHERE id=v_prod;
  IF (SELECT precio_unitario FROM pedido_detalles WHERE pedido_id=v_pedido)<>v_precio THEN
    RAISE EXCEPTION 'No se preservo el precio historico';
  END IF;

  INSERT INTO pagos(pedido_id,metodo,estado,monto,referencia_externa)
    VALUES(v_pedido,'tarjeta','confirmado',v_total,'PRUEBA-TRANSACCIONAL');
  SELECT pagado INTO v_pagado FROM vw_cuenta_pedidos WHERE pedido_id=v_pedido;
  IF v_pagado<>v_total THEN RAISE EXCEPTION 'Fallo vista de cobro'; END IF;

  BEGIN
    INSERT INTO pagos(pedido_id,metodo,estado,monto)
      VALUES(v_pedido,'tarjeta','confirmado',1);
    RAISE EXCEPTION 'FALLO: se permitio sobrepago';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM='FALLO: se permitio sobrepago' THEN RAISE; END IF;
  END;

  v_inicio := date_trunc('day',now()) + interval '7 days 18 hours';
  INSERT INTO reservas(mesa_id,nombre_contacto,personas,inicio,fin,estado)
    VALUES(v_mesa,'Persona de prueba',2,v_inicio,v_inicio+interval '2 hours','confirmada')
    RETURNING id INTO v_reserva;
  BEGIN
    INSERT INTO reservas(mesa_id,nombre_contacto,personas,inicio,fin,estado)
      VALUES(v_mesa,'Otro contacto',2,v_inicio+interval '1 hour',v_inicio+interval '3 hours','confirmada');
    RAISE EXCEPTION 'FALLO: se permitio reserva traslapada';
  EXCEPTION WHEN exclusion_violation THEN
    RAISE NOTICE 'OK: doble reserva bloqueada';
  END;

  BEGIN
    INSERT INTO reservas(mesa_id,nombre_contacto,personas,inicio,fin)
      VALUES(v_mesa,'Exceso',20,v_inicio+interval '1 day',v_inicio+interval '1 day 2 hours');
    RAISE EXCEPTION 'FALLO: se permitio exceso de capacidad';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM='FALLO: se permitio exceso de capacidad' THEN RAISE; END IF;
  END;

  RAISE NOTICE 'OK: precio historico, total, cobros y reservas verificados (pedido %, reserva %)',
    v_pedido,v_reserva;
END;
$$;
ROLLBACK;
