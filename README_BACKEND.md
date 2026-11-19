# Mesa Cero | Backend Java con Jakarta Servlets + PostgreSQL

Paquete **separado del frontend** de `MesaCero-Inicio`. No sobreescribe Angular ni se mezcla con el antiguo proyecto `MesaCero` (GlassFish). Pensado para la base de datos `mesa_cero`, esquema `mesacero`, con las 16 tablas del paquete `MesaCero-PostgreSQL` entregado previamente.

## ¿Qué contiene?

- `backend/`: proyecto Maven WAR para Java 17+, Tomcat 10.1 / GlassFish 7.
- `database/`: copia de los cinco SQL originales de PostgreSQL para instalar o examinar el esquema completo.
- `backend/src/main/java/`: 18 Servlets reales, filtros de CORS y acceso administrativo, pool HikariCP, POJO/DTO para carta y mesas, JSON con Jackson y consultas JDBC preparadas.
- `integracion-angular/`: servicio TypeScript de ejemplo, proxy Angular y guía para enlazar sin romper el prototipo.
- `docs/RUBRICA_Y_ARQUITECTURA.md`: mapa de tablas, métodos HTTP, limitaciones de seguridad y pruebas.
- `docs/MesaCero_Vistas_Triggers.pdf`: explicación de las vistas y triggers de PostgreSQL.

## 1. Requisitos

1. JDK 17 o 21, Maven 3.9+, PostgreSQL 15+ y Apache Tomcat 10.1 (o GlassFish 7, compatible con Jakarta Servlet 6).
2. Tener ejecutados, por ese orden, los SQL incluidos en `database/`:
   `01_esquema.sql`, `02_funciones_triggers.sql`, `03_vistas.sql`, `04_datos_demo.sql`.
3. **Usar la MISMA base que esos scripts**. En este proyecto se llama `mesa_cero`, y su esquema de tablas se llama `mesacero`.

En PowerShell, desde la carpeta raíz de este paquete:

```powershell
psql -U postgres -c 'CREATE DATABASE mesa_cero;'
psql -U postgres -d mesa_cero -f database/01_esquema.sql
psql -U postgres -d mesa_cero -f database/02_funciones_triggers.sql
psql -U postgres -d mesa_cero -f database/03_vistas.sql
psql -U postgres -d mesa_cero -f database/04_datos_demo.sql
```

No ejecutes `01_esquema.sql` otra vez sobre una base ya inicializada sin revisar primero: podría fallar por tablas existentes. Si ya instalaste la base, **no necesitas recrearla**.

## 2. Configurar el backend localmente

Antes de iniciar Tomcat/GlassFish, define variables de entorno en **la misma terminal PowerShell** desde la que ejecutarás el servidor:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/mesa_cero'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'PON_AQUI_TU_CONTRASENA_LOCAL'
$env:API_ADMIN_KEY = 'CAMBIA-ESTO-POR-UNA-CLAVE-ALEATORIA-DE-32-CARACTERES'
$env:CORS_ORIGINS = 'http://localhost:4200'
```

- Para clase basta el usuario `postgres` en tu instalación local, **no** en producción. Luego crea un usuario limitado de PostgreSQL para la app.
- **Nunca** subas una contraseña real, `API_ADMIN_KEY` ni archivos `.env` a GitHub.
- `API_ADMIN_KEY` es **solo para el entorno local de clase**. No debe ponerse en el código de Angular, pues cualquier visitante podría leerla. Para publicar el sistema hay que implementar sesiones, autenticación real y roles.
- Si Tomcat se abre desde otra aplicación/servicio, asegúrate de que recibe esas variables; PowerShell no las transfiere a procesos que ya están abiertos.

## 3. Compilar y ejecutar

Dentro de `backend`:

```powershell
mvn clean package
```

Obtendrás `backend/target/mesacero-api.war`. Despliega ese WAR en Tomcat 10.1 (carpeta `webapps/`) o en GlassFish 7.

Si el contexto del servidor es el nombre del WAR, las rutas principales son:

| URL completa local | Para qué sirve |
| --- | --- |
| `http://localhost:8080/mesacero-api/api/health` | Comprobar conexión a BD |
| `http://localhost:8080/mesacero-api/api/catalogo` | Carta pública para Angular |
| `http://localhost:8080/mesacero-api/api/estado-mesas` | Mesas y estado calculado |
| `http://localhost:8080/mesacero-api/api/reservas` | POST para crear una reserva |
| `http://localhost:8080/mesacero-api/api/admin/productos` | CRUD protegido de platillos |
| `http://localhost:8080/mesacero-api/api/admin/comandas` | POST atómico pedido + detalles |
| `http://localhost:8080/mesacero-api/api/admin/pagos` | GET / POST de cobros |
| `http://localhost:8080/mesacero-api/api/admin/reportes?vista=vw_dashboard_resumen` | Dashboard y otras vistas |

**Importante**: si publicas en un contexto distinto, la URL base cambiará.

## 4. Probar HTTP en PowerShell

### GET público

```powershell
Invoke-RestMethod http://localhost:8080/mesacero-api/api/health
Invoke-RestMethod http://localhost:8080/mesacero-api/api/catalogo
```

### POST protegido: ejemplo con una nueva categoría

```powershell
$base = 'http://localhost:8080/mesacero-api/api/admin/categorias'
$headers = @{ 'X-Admin-Key' = $env:API_ADMIN_KEY }
$nuevo = Invoke-RestMethod -Uri $base -Method Post -Headers $headers -ContentType 'application/json' -Body '{"nombre":"Especiales","orden":8,"activa":true}'
$nuevo.id
```

### GET, PATCH, PUT y DELETE

```powershell
Invoke-RestMethod -Uri "$base/$($nuevo.id)" -Method Get -Headers $headers
Invoke-RestMethod -Uri "$base/$($nuevo.id)" -Method Patch -Headers $headers -ContentType 'application/json' -Body '{"orden":9}'
Invoke-RestMethod -Uri "$base/$($nuevo.id)" -Method Put -Headers $headers -ContentType 'application/json' -Body '{"nombre":"Especiales","orden":10,"activa":true}'
Invoke-RestMethod -Uri "$base/$($nuevo.id)" -Method Delete -Headers $headers
```

El `DELETE` de este ejemplo elimina únicamente la categoría de prueba, si no hay productos relacionados con ella. Nunca borres datos reales para demostrar el método.

### POST de comanda (la base determina el precio)

```powershell
$json = @{mesaId = 1; items = @(@{productoId = 1; cantidad = 2}, @{productoId = 7; cantidad = 1})} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri http://localhost:8080/mesacero-api/api/admin/comandas -Method Post -Headers $headers -ContentType 'application/json' -Body $json
```

Para conocer otros ejemplos y el mapeo de tablas, consulta `docs/RUBRICA_Y_ARQUITECTURA.md`.

## 5. Conexión Angular

Tu proyecto Angular **ya tiene `provideHttpClient()`** en `app.config.ts`. Solo necesitas conectar el servicio y ejecutar el servidor de desarrollo con el proxy. Sigue `integracion-angular/COMO_CONECTAR.md`.

## 6. Advertencias técnicas honestas

- Este es un backend implementado **en código** y preparado para compilar. Se verificó la sintaxis de los archivos Java; no se compiló con dependencias ni se ejecutó contra PostgreSQL/Tomcat porque este entorno no tiene Maven ni servidor de PostgreSQL. **Prueba el WAR y los endpoints en tu equipo** antes de considerarlo terminado.
- Los 11 CRUD administrativos son una base académica; no se deben exponer públicamente. Antes de producción faltan login real, autorización por rol y dueño del recurso, validación de mayor detalle, límites de solicitudes, pruebas integradas y auditorías.
- Las funciones críticas `fn_preparar_detalle`, `fn_recalcular_pedido`, `fn_validar_pago` y las reservas sin traslape siguen ejecutándose dentro de PostgreSQL.
- No se incluye cifrado de archivos: las capturas enseñan una búsqueda sobre ese tema, pero no demuestra que sea parte de la rúbrica.
