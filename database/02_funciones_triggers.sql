-- Funciones PL/pgSQL y triggers. Ejecutar despues de 01_esquema.sql.
BEGIN;
SET LOCAL search_path TO mesacero, public;

-- T01: fecha de ultima modificacion en tablas mutables.
CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  NEW.actualizado_en := now();
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_usuarios_fecha BEFORE UPDATE ON usuarios FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_zonas_fecha BEFORE UPDATE ON zonas FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_mesas_fecha BEFORE UPDATE ON mesas FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_categorias_fecha BEFORE UPDATE ON categorias FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_productos_fecha BEFORE UPDATE ON productos FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_carritos_fecha BEFORE UPDATE ON carritos FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_reservas_fecha BEFORE UPDATE ON reservas FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_pedidos_fecha BEFORE UPDATE ON pedidos FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_turnos_fecha BEFORE UPDATE ON turnos_caja FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER trg_pagos_fecha BEFORE UPDATE ON pagos FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- T02: coherencia de las reservas (el EXCLUDE evita el doble apartado concurrente).
CREATE OR REPLACE FUNCTION fn_validar_reserva() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_capacidad smallint; v_activa boolean;
BEGIN
  SELECT m.capacidad, (m.estado_manual = 'activa' AND z.activa)
    INTO v_capacidad, v_activa
    FROM mesas m JOIN zonas z ON z.id = m.zona_id
    WHERE m.id = NEW.mesa_id;
  IF v_capacidad IS NULL THEN RAISE EXCEPTION 'Mesa inexistente'; END IF;
  IF NEW.personas > v_capacidad THEN
    RAISE EXCEPTION 'La mesa admite % personas, no %', v_capacidad, NEW.personas;
  END IF;
  IF NEW.estado IN ('pendiente','confirmada') AND NOT v_activa THEN
    RAISE EXCEPTION 'La mesa o su zona se encuentra fuera de servicio';
  END IF;
  IF NEW.fin - NEW.inicio > interval '8 hours' THEN
    RAISE EXCEPTION 'Una reserva no puede durar mas de ocho horas';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER trg_reservas_validar BEFORE INSERT OR UPDATE ON reservas
FOR EACH ROW EXECUTE FUNCTION fn_validar_reserva();

-- T03: un carrito abierto solo puede contener articulos disponibles.
CREATE OR REPLACE FUNCTION fn_validar_item_carrito() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_estado text; v_disponible boolean;
BEGIN
  SELECT estado INTO v_estado FROM carritos WHERE id = NEW.carrito_id;
  IF v_estado <> 'abierto' THEN
    RAISE EXCEPTION 'No se puede modificar un carrito %', v_estado;
  END IF;
  SELECT (activo AND disponible) INTO v_disponible FROM productos WHERE id = NEW.producto_id;
  IF NOT COALESCE(v_disponible, false) THEN
    RAISE EXCEPTION 'Producto % no disponible', NEW.producto_id;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER trg_carrito_item_validar BEFORE INSERT OR UPDATE ON carrito_items
FOR EACH ROW EXECUTE FUNCTION fn_validar_item_carrito();

-- T04: impedir pedidos en mesas bloqueadas o vinculados a una reserva ajena.
CREATE OR REPLACE FUNCTION fn_validar_pedido() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_habilitada boolean; v_mesa_reserva bigint;
BEGIN
  IF TG_OP = 'UPDATE' THEN
    IF NEW.mesa_id IS DISTINCT FROM OLD.mesa_id THEN
      RAISE EXCEPTION 'No se puede cambiar la mesa de un pedido ya creado';
    END IF;
  END IF;
  IF TG_OP = 'INSERT' THEN
    SELECT (m.estado_manual='activa' AND z.activa) INTO v_habilitada
      FROM mesas m JOIN zonas z ON z.id=m.zona_id WHERE m.id=NEW.mesa_id;
    IF NOT COALESCE(v_habilitada, false) THEN
      RAISE EXCEPTION 'Mesa no habilitada';
    END IF;
  END IF;
  IF NEW.reserva_id IS NOT NULL THEN
    SELECT mesa_id INTO v_mesa_reserva FROM reservas WHERE id=NEW.reserva_id;
    IF v_mesa_reserva IS DISTINCT FROM NEW.mesa_id THEN
      RAISE EXCEPTION 'La reserva pertenece a otra mesa';
    END IF;
  END IF;
  IF TG_OP = 'UPDATE' THEN
    IF NEW.estado = 'cancelado' AND OLD.estado <> 'cancelado'
      AND EXISTS (SELECT 1 FROM pagos WHERE pedido_id=NEW.id AND estado='confirmado') THEN
      RAISE EXCEPTION 'No se puede cancelar un pedido con pagos confirmados';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER trg_pedidos_validar BEFORE INSERT OR UPDATE ON pedidos
FOR EACH ROW EXECUTE FUNCTION fn_validar_pedido();

-- T05: precio y nombre historicos al agregar cada renglon; pedido inmutable al cobrar.
CREATE OR REPLACE FUNCTION fn_preparar_detalle() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_pedido_id bigint; v_estado text; v_nombre varchar(140);
        v_precio numeric(12,2); v_disponible boolean;
BEGIN
  v_pedido_id := CASE WHEN TG_OP='DELETE' THEN OLD.pedido_id ELSE NEW.pedido_id END;
  SELECT estado INTO v_estado FROM pedidos WHERE id=v_pedido_id FOR UPDATE;
  IF v_estado NOT IN ('pendiente','en_preparacion') THEN
    RAISE EXCEPTION 'Solo se modifican detalles de pedidos pendientes o en preparacion';
  END IF;
  IF EXISTS (SELECT 1 FROM pagos WHERE pedido_id=v_pedido_id AND estado='confirmado') THEN
    RAISE EXCEPTION 'No se puede editar un pedido con pagos confirmados';
  END IF;
  IF TG_OP='DELETE' THEN RETURN OLD; END IF;
  IF TG_OP='UPDATE' THEN
    IF NEW.pedido_id <> OLD.pedido_id OR NEW.producto_id <> OLD.producto_id THEN
      RAISE EXCEPTION 'No se puede trasladar un renglon; borralo y agregalo de nuevo';
    END IF;
    NEW.precio_unitario := OLD.precio_unitario;
    NEW.producto_nombre := OLD.producto_nombre;
    RETURN NEW;
  END IF;
  SELECT nombre, precio, (disponible AND activo)
    INTO v_nombre,v_precio,v_disponible FROM productos WHERE id=NEW.producto_id;
  IF NOT COALESCE(v_disponible, false) THEN
    RAISE EXCEPTION 'El producto % ya no esta disponible',NEW.producto_id;
  END IF;
  NEW.producto_nombre := v_nombre;
  NEW.precio_unitario := v_precio;
  RETURN NEW;
END;
$$;
CREATE TRIGGER trg_detalle_preparar BEFORE INSERT OR UPDATE OR DELETE ON pedido_detalles
FOR EACH ROW EXECUTE FUNCTION fn_preparar_detalle();

-- T06: calcular los totales en la BD, sin aceptar el precio del navegador.
CREATE OR REPLACE FUNCTION fn_recalcular_pedido() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_pedido_id bigint;
BEGIN
  v_pedido_id := CASE WHEN TG_OP='DELETE' THEN OLD.pedido_id ELSE NEW.pedido_id END;
  UPDATE pedidos p SET
    subtotal = COALESCE((SELECT sum(d.subtotal) FROM pedido_detalles d WHERE d.pedido_id=v_pedido_id),0),
    total = COALESCE((SELECT sum(d.subtotal) FROM pedido_detalles d WHERE d.pedido_id=v_pedido_id),0)
    WHERE p.id=v_pedido_id;
  RETURN NULL;
END;
$$;
CREATE TRIGGER trg_detalle_recalcular AFTER INSERT OR UPDATE OR DELETE ON pedido_detalles
FOR EACH ROW EXECUTE FUNCTION fn_recalcular_pedido();

-- T07: cobrar sin exceder el saldo y serializar pagos concurrentes por pedido.
CREATE OR REPLACE FUNCTION fn_validar_pago() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_total numeric(12,2); v_estado text; v_pagado numeric(12,2);
        v_estado_turno text;
BEGIN
  IF TG_OP='UPDATE' THEN
    IF NEW.pedido_id <> OLD.pedido_id OR
      (OLD.estado='confirmado' AND
       (NEW.monto <> OLD.monto OR NEW.metodo <> OLD.metodo OR
        NEW.turno_caja_id IS DISTINCT FROM OLD.turno_caja_id)) THEN
      RAISE EXCEPTION 'No se permite alterar el importe ni el origen de un pago confirmado';
    END IF;
  END IF;
  -- FOR UPDATE hace que dos cobros concurrentes se procesen en orden.
  SELECT total,estado INTO v_total,v_estado FROM pedidos WHERE id=NEW.pedido_id FOR UPDATE;
  IF NEW.estado='confirmado' THEN
    IF v_estado='cancelado' OR v_total <= 0 THEN
      RAISE EXCEPTION 'El pedido no tiene un saldo cobrable';
    END IF;
    SELECT COALESCE(sum(monto),0) INTO v_pagado FROM pagos
      WHERE pedido_id=NEW.pedido_id AND estado='confirmado'
        AND (TG_OP='INSERT' OR id <> NEW.id);
    IF v_pagado + NEW.monto > v_total THEN
      RAISE EXCEPTION 'El pago excede el total del pedido (saldo disponible %)',v_total-v_pagado;
    END IF;
    IF NEW.metodo='efectivo' THEN
      IF NEW.turno_caja_id IS NULL THEN
        RAISE EXCEPTION 'Un cobro en efectivo requiere un turno de caja abierto';
      END IF;
      SELECT estado INTO v_estado_turno FROM turnos_caja
        WHERE id=NEW.turno_caja_id FOR UPDATE;
      IF v_estado_turno IS DISTINCT FROM 'abierto' THEN
        RAISE EXCEPTION 'El turno de caja no se encuentra abierto';
      END IF;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER trg_pagos_validar BEFORE INSERT OR UPDATE ON pagos
FOR EACH ROW EXECUTE FUNCTION fn_validar_pago();

-- T08: traza de cambios de estado de cada comanda.
CREATE OR REPLACE FUNCTION fn_historial_pedido() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP='INSERT' THEN
    INSERT INTO pedido_historial(pedido_id,estado_anterior,estado_nuevo)
      VALUES(NEW.id,NULL,NEW.estado);
  ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
    INSERT INTO pedido_historial(pedido_id,estado_anterior,estado_nuevo)
      VALUES(NEW.id,OLD.estado,NEW.estado);
  END IF;
  RETURN NULL;
END;
$$;
CREATE TRIGGER trg_pedidos_historial AFTER INSERT OR UPDATE OF estado ON pedidos
FOR EACH ROW EXECUTE FUNCTION fn_historial_pedido();

-- T09: auditoria de cambios criticos (sin contrasenas ni datos bancarios).
-- El Servlet puede establecer SET LOCAL mesacero.actor_id='123' en su transaccion.
CREATE OR REPLACE FUNCTION fn_auditar_cambio() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_actor text; v_registro bigint;
BEGIN
  v_actor := current_setting('mesacero.actor_id',true);
  v_registro := CASE WHEN TG_OP='DELETE' THEN OLD.id ELSE NEW.id END;
  INSERT INTO auditoria(tabla,registro_id,operacion,antes,despues,actor_id)
  VALUES(TG_TABLE_NAME,v_registro,TG_OP,
         CASE WHEN TG_OP='INSERT' THEN NULL ELSE to_jsonb(OLD) END,
         CASE WHEN TG_OP='DELETE' THEN NULL ELSE to_jsonb(NEW) END,
         CASE WHEN v_actor ~ '^[0-9]+$' THEN v_actor::bigint ELSE NULL END);
  RETURN NULL;
END;
$$;
CREATE TRIGGER trg_productos_auditar AFTER INSERT OR UPDATE OR DELETE ON productos
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();
CREATE TRIGGER trg_mesas_auditar AFTER INSERT OR UPDATE OR DELETE ON mesas
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();
CREATE TRIGGER trg_reservas_auditar AFTER INSERT OR UPDATE OR DELETE ON reservas
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();
CREATE TRIGGER trg_pedidos_auditar AFTER INSERT OR UPDATE OR DELETE ON pedidos
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();
CREATE TRIGGER trg_pagos_auditar AFTER INSERT OR UPDATE OR DELETE ON pagos
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

COMMIT;
