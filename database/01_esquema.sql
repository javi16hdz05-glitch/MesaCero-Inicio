-- MESA CERO | PostgreSQL 15+ | Ejecutar dentro de la BD mesa_cero.
-- Orden: 01_esquema, 02_funciones_triggers, 03_vistas, 04_datos_demo.
-- No ejecutar como un usuario de la aplicacion: requiere permiso CREATE EXTENSION.
BEGIN;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE SCHEMA IF NOT EXISTS mesacero;
SET LOCAL search_path TO mesacero, public;

CREATE TABLE roles (
  id smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre varchar(30) NOT NULL UNIQUE CHECK (nombre IN ('cliente','mesero','administrador','cajero'))
);

CREATE TABLE usuarios (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  rol_id smallint NOT NULL REFERENCES roles(id),
  nombre varchar(120) NOT NULL CHECK (length(btrim(nombre)) > 1),
  correo varchar(180) NOT NULL,
  telefono varchar(25),
  password_hash text NOT NULL CHECK (length(password_hash) >= 20),
  activo boolean NOT NULL DEFAULT true,
  creado_en timestamptz NOT NULL DEFAULT now(),
  actualizado_en timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_usuarios_correo_normalizado ON usuarios (lower(btrim(correo)));

CREATE TABLE tokens_recuperacion (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id bigint NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  token_hash char(64) NOT NULL UNIQUE,
  expira_en timestamptz NOT NULL,
  usado_en timestamptz,
  creado_en timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_token_vigencia CHECK (expira_en > creado_en)
);

CREATE TABLE zonas (
  id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre varchar(80) NOT NULL UNIQUE,
  descripcion text,
  activa boolean NOT NULL DEFAULT true,
  actualizado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE mesas (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  zona_id integer NOT NULL REFERENCES zonas(id),
  numero integer NOT NULL UNIQUE CHECK (numero > 0),
  capacidad smallint NOT NULL CHECK (capacidad BETWEEN 1 AND 30),
  estado_manual varchar(20) NOT NULL DEFAULT 'activa'
    CHECK (estado_manual IN ('activa', 'fuera_servicio')),
  actualizado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE categorias (
  id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre varchar(60) NOT NULL UNIQUE,
  orden smallint NOT NULL DEFAULT 0,
  activa boolean NOT NULL DEFAULT true,
  actualizado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE productos (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  categoria_id integer NOT NULL REFERENCES categorias(id),
  nombre varchar(140) NOT NULL,
  descripcion text NOT NULL DEFAULT '',
  precio numeric(12,2) NOT NULL CHECK (precio >= 0),
  foto_url text NOT NULL DEFAULT '',
  etiqueta varchar(40),
  disponible boolean NOT NULL DEFAULT true,
  activo boolean NOT NULL DEFAULT true,
  orden integer NOT NULL DEFAULT 0,
  creado_en timestamptz NOT NULL DEFAULT now(),
  actualizado_en timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_producto_categoria_nombre UNIQUE (categoria_id, nombre)
);

CREATE TABLE carritos (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id bigint REFERENCES usuarios(id) ON DELETE CASCADE,
  sesion_anonima_hash char(64) UNIQUE,
  estado varchar(15) NOT NULL DEFAULT 'abierto'
    CHECK (estado IN ('abierto','convertido','abandonado')),
  creado_en timestamptz NOT NULL DEFAULT now(),
  actualizado_en timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_propietario_carrito CHECK
    ((usuario_id IS NOT NULL) <> (sesion_anonima_hash IS NOT NULL))
);
CREATE UNIQUE INDEX uq_carrito_abierto_usuario ON carritos (usuario_id)
  WHERE estado = 'abierto' AND usuario_id IS NOT NULL;
CREATE UNIQUE INDEX uq_carrito_abierto_anonimo ON carritos (sesion_anonima_hash)
  WHERE estado = 'abierto' AND sesion_anonima_hash IS NOT NULL;

CREATE TABLE carrito_items (
  carrito_id bigint NOT NULL REFERENCES carritos(id) ON DELETE CASCADE,
  producto_id bigint NOT NULL REFERENCES productos(id),
  cantidad integer NOT NULL CHECK (cantidad BETWEEN 1 AND 99),
  PRIMARY KEY (carrito_id, producto_id)
);

CREATE TABLE reservas (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id bigint REFERENCES usuarios(id) ON DELETE SET NULL,
  mesa_id bigint NOT NULL REFERENCES mesas(id),
  nombre_contacto varchar(120) NOT NULL CHECK (length(btrim(nombre_contacto)) > 1),
  telefono_contacto varchar(25),
  personas smallint NOT NULL CHECK (personas BETWEEN 1 AND 30),
  inicio timestamptz NOT NULL,
  fin timestamptz NOT NULL,
  estado varchar(20) NOT NULL DEFAULT 'pendiente'
    CHECK (estado IN ('pendiente','confirmada','cumplida','cancelada','no_asistio')),
  notas text,
  creado_en timestamptz NOT NULL DEFAULT now(),
  actualizado_en timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_periodo_reserva CHECK (fin > inicio),
  CONSTRAINT ex_reserva_sin_traslape EXCLUDE USING gist (
    mesa_id WITH =, tstzrange(inicio, fin, '[)') WITH &&
  ) WHERE (estado IN ('pendiente','confirmada'))
);

CREATE TABLE pedidos (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  mesa_id bigint NOT NULL REFERENCES mesas(id),
  usuario_id bigint REFERENCES usuarios(id) ON DELETE SET NULL,
  mesero_id bigint REFERENCES usuarios(id) ON DELETE SET NULL,
  reserva_id bigint REFERENCES reservas(id) ON DELETE SET NULL,
  estado varchar(20) NOT NULL DEFAULT 'pendiente'
    CHECK (estado IN ('pendiente','en_preparacion','listo','entregado','completado','cancelado')),
  notas text,
  subtotal numeric(12,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
  total numeric(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
  creado_en timestamptz NOT NULL DEFAULT now(),
  actualizado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE pedido_detalles (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id bigint NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT,
  producto_id bigint NOT NULL REFERENCES productos(id),
  producto_nombre varchar(140) NOT NULL,
  cantidad integer NOT NULL CHECK (cantidad BETWEEN 1 AND 99),
  precio_unitario numeric(12,2) NOT NULL CHECK (precio_unitario >= 0),
  subtotal numeric(12,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
  observaciones text,
  UNIQUE (pedido_id, producto_id)
);

CREATE TABLE turnos_caja (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cajero_id bigint NOT NULL REFERENCES usuarios(id),
  abierto_en timestamptz NOT NULL DEFAULT now(),
  cerrado_en timestamptz,
  apertura_efectivo numeric(12,2) NOT NULL DEFAULT 0 CHECK (apertura_efectivo >= 0),
  cierre_declarado numeric(12,2) CHECK (cierre_declarado >= 0),
  estado varchar(12) NOT NULL DEFAULT 'abierto' CHECK (estado IN ('abierto','cerrado')),
  actualizado_en timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_cierre_turno CHECK
   ((estado = 'abierto' AND cerrado_en IS NULL) OR
    (estado = 'cerrado' AND cerrado_en IS NOT NULL AND cerrado_en >= abierto_en))
);
CREATE UNIQUE INDEX uq_turno_abierto_por_cajero ON turnos_caja (cajero_id) WHERE estado = 'abierto';

CREATE TABLE pagos (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id bigint NOT NULL REFERENCES pedidos(id),
  turno_caja_id bigint REFERENCES turnos_caja(id),
  registrado_por bigint REFERENCES usuarios(id) ON DELETE SET NULL,
  metodo varchar(20) NOT NULL CHECK (metodo IN ('efectivo','tarjeta','transferencia')),
  estado varchar(15) NOT NULL DEFAULT 'pendiente'
    CHECK (estado IN ('pendiente','confirmado','rechazado','reembolsado')),
  monto numeric(12,2) NOT NULL CHECK (monto > 0),
  referencia_externa varchar(150) UNIQUE,
  creado_en timestamptz NOT NULL DEFAULT now(),
  actualizado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE pedido_historial (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id bigint NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
  estado_anterior varchar(20),
  estado_nuevo varchar(20) NOT NULL,
  cambiado_en timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE auditoria (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tabla varchar(80) NOT NULL,
  registro_id bigint,
  operacion varchar(10) NOT NULL,
  antes jsonb,
  despues jsonb,
  actor_id bigint,
  ocurrido_en timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_producto_categoria ON productos (categoria_id, activo, disponible);
CREATE INDEX idx_reservas_usuario_fecha ON reservas (usuario_id, inicio DESC);
CREATE INDEX idx_reservas_mesa_inicio ON reservas (mesa_id, inicio);
CREATE INDEX idx_pedidos_mesa_estado ON pedidos (mesa_id, estado);
CREATE INDEX idx_pedidos_usuario_fecha ON pedidos (usuario_id, creado_en DESC);
CREATE INDEX idx_pedidos_creado ON pedidos (creado_en DESC);
CREATE INDEX idx_pedido_detalles_producto ON pedido_detalles (producto_id);
CREATE INDEX idx_pagos_pedido_estado ON pagos (pedido_id, estado);
CREATE INDEX idx_pagos_fecha_confirmados ON pagos (creado_en DESC) WHERE estado = 'confirmado';
CREATE INDEX idx_pagos_turno ON pagos (turno_caja_id);
CREATE INDEX idx_historial_pedido ON pedido_historial (pedido_id, cambiado_en);
CREATE INDEX idx_auditoria_tabla_fecha ON auditoria (tabla, ocurrido_en DESC);
CREATE INDEX idx_tokens_usuario ON tokens_recuperacion (usuario_id, expira_en);

-- Los permisos de produccion deben concederse solo a un rol de backend.
REVOKE ALL ON SCHEMA mesacero FROM PUBLIC;
COMMIT;
