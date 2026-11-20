-- Crear la base primero:  CREATE DATABASE mesacero;
-- Luego ejecutar este script conectado a esa base.

DROP TABLE IF EXISTS comanda_items, comandas, reservas, productos, mesas CASCADE;

CREATE TABLE mesas (
  id        SERIAL PRIMARY KEY,
  numero    INT NOT NULL UNIQUE CHECK (numero > 0),
  capacidad INT NOT NULL CHECK (capacidad > 0),
  estado    VARCHAR(20) NOT NULL DEFAULT 'disponible'
            CHECK (estado IN ('disponible','ocupada','reservada'))
);

CREATE TABLE productos (
  id          SERIAL PRIMARY KEY,
  nombre      VARCHAR(100) NOT NULL,
  categoria   VARCHAR(20) NOT NULL
              CHECK (categoria IN ('Oaxaqueños','Fuertes','Bebidas','Postres')),
  precio      NUMERIC(10,2) NOT NULL CHECK (precio > 0),
  disponible  BOOLEAN NOT NULL DEFAULT TRUE,
  descripcion TEXT,
  foto        VARCHAR(200),
  etiqueta    VARCHAR(30)
);

CREATE TABLE comandas (
  id      SERIAL PRIMARY KEY,
  mesa_id INT NOT NULL REFERENCES mesas(id),
  total   NUMERIC(10,2) NOT NULL DEFAULT 0,
  fecha   TIMESTAMPTZ NOT NULL DEFAULT now(),
  estado  VARCHAR(20) NOT NULL DEFAULT 'en_preparacion'
          CHECK (estado IN ('pendiente','en_preparacion','completada'))
);

CREATE TABLE comanda_items (
  id          SERIAL PRIMARY KEY,
  comanda_id  INT NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
  producto_id INT NOT NULL REFERENCES productos(id),
  cantidad    INT NOT NULL CHECK (cantidad > 0)
);

CREATE TABLE reservas (
  id       SERIAL PRIMARY KEY,
  mesa_id  INT NOT NULL REFERENCES mesas(id),
  fecha    DATE NOT NULL,
  hora     TIME NOT NULL,
  personas INT NOT NULL CHECK (personas > 0),
  zona     VARCHAR(60)
);

-- Datos iniciales (los mismos que tenía el frontend en memoria)
INSERT INTO mesas (numero, capacidad, estado) VALUES
  (1,2,'disponible'),(2,4,'ocupada'),(3,4,'disponible'),
  (4,6,'disponible'),(5,2,'disponible'),(6,6,'ocupada');

INSERT INTO productos (nombre, categoria, precio, disponible, descripcion, foto, etiqueta) VALUES
  ('Tlayuda especial','Oaxaqueños',185,TRUE,'Tasajo, quesillo, frijol negro y aguacate.','assets/tlayuda.png','Popular'),
  ('Salmón cítrico','Fuertes',245,TRUE,'Salmón asado con cítricos frescos.','assets/salmon-raw.png','Nuevo'),
  ('Pasta de la casa','Fuertes',198,TRUE,'Pasta artesanal con salsa y hierbas.','assets/pasta.png',NULL),
  ('Enchiladas oaxaqueñas','Oaxaqueños',165,TRUE,'Pollo, salsa de chile y queso.','assets/enchiladas.png',NULL),
  ('Filete al ajo','Fuertes',295,TRUE,'Corte a la parrilla con puré.','assets/filete.png',NULL),
  ('Cheesecake de frutos rojos','Postres',135,TRUE,'Postre cremoso y frutos frescos.','assets/cheesecake.png',NULL),
  ('Agua de jamaica','Bebidas',55,TRUE,'Refrescante y natural.','assets/jamaica.png',NULL);
