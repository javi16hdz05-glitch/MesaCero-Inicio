-- Datos no sensibles de demostracion; es seguro ejecutar el archivo varias veces.
BEGIN;
SET LOCAL search_path TO mesacero, public;

INSERT INTO roles(nombre) VALUES
('cliente'),('mesero'),('administrador'),('cajero')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO zonas(nombre,descripcion) VALUES
('Salon interior','Mesas bajo techo'),('Terraza','Area al aire libre')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO mesas(zona_id,numero,capacidad)
SELECT z.id,v.numero,v.capacidad FROM
 (VALUES ('Salon interior',1,2),('Salon interior',2,4),
         ('Salon interior',3,4),('Salon interior',4,6),
         ('Terraza',5,2),('Terraza',6,6)) v(zona,numero,capacidad)
 JOIN zonas z ON z.nombre=v.zona
ON CONFLICT (numero) DO NOTHING;

INSERT INTO categorias(nombre,orden) VALUES
('Oaxaqueños',1),('Fuertes',2),('Bebidas',3),('Postres',4)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO productos(categoria_id,nombre,descripcion,precio,foto_url,etiqueta,orden)
SELECT c.id,v.nombre,v.descripcion,v.precio,v.foto,v.etiqueta,v.orden FROM
 (VALUES
  ('Oaxaqueños','Tlayuda especial','Tasajo, quesillo, frijol negro y aguacate.',185.00,'assets/tlayuda.png','Popular',1),
  ('Fuertes','Salmón cítrico','Salmón asado con cítricos frescos.',245.00,'assets/salmon-raw.png','Nuevo',2),
  ('Fuertes','Pasta de la casa','Pasta artesanal con salsa y hierbas.',198.00,'assets/pasta.png',NULL,3),
  ('Oaxaqueños','Enchiladas oaxaqueñas','Pollo, salsa de chile y queso.',165.00,'assets/enchiladas.png',NULL,4),
  ('Fuertes','Filete al ajo','Corte a la parrilla con puré.',295.00,'assets/filete.png',NULL,5),
  ('Postres','Cheesecake de frutos rojos','Postre cremoso y frutos frescos.',135.00,'assets/cheesecake.png',NULL,6),
  ('Bebidas','Agua de jamaica','Refrescante y natural.',55.00,'assets/jamaica.png',NULL,7)
 ) v(categoria,nombre,descripcion,precio,foto,etiqueta,orden)
 JOIN categorias c ON c.nombre=v.categoria
ON CONFLICT (categoria_id,nombre) DO NOTHING;

-- No incluye usuarios ni contrasenas predeterminadas: crear cuentas desde el backend.
-- Tampoco incluye pagos ficticios; las vistas de ventas empiezan en cero.
COMMIT;
