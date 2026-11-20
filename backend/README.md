# Mesa Cero — Backend (Servlets + DAO + PostgreSQL)

API REST hecha con Jakarta Servlet, JDBC y PostgreSQL. Angular la consume por HTTP; nunca se conecta directo a la base.

## Estructura

```text
backend/
├── pom.xml
├── sql/schema.sql                      # tablas + datos iniciales
└── src/main/java/mx/mesacero/
    ├── model/    Mesa, Producto, Comanda, ItemComanda, Reserva   (POJO)
    ├── dao/      CrudDAO + MesaDAO, ProductoDAO, ComandaDAO, ReservaDAO
    ├── servlet/  BaseServlet + MesaServlet, ProductoServlet, ComandaServlet, ReservaServlet
    ├── filter/   CorsFilter
    └── util/     ConexionBD
```

## Métodos de cada servlet

| Método | Ruta | Qué hace |
| --- | --- | --- |
| `doGet` | `/api/{recurso}` o `/api/{recurso}/{id}` | Lista todo o devuelve uno |
| `doPost` | `/api/{recurso}` | Crea (responde 201) |
| `doPut` | `/api/{recurso}/{id}` | Reemplaza el registro completo |
| `doPatch` | `/api/{recurso}/{id}` | Cambia solo los campos enviados |
| `doDelete` | `/api/{recurso}/{id}` | Elimina (responde 204) |

Recursos: `mesas`, `productos`, `comandas`, `reservas`.

## Puesta en marcha

1. Crear la base y cargar el script:

```powershell
psql -U postgres -c "CREATE DATABASE mesacero;"
psql -U postgres -d mesacero -f sql/schema.sql
```

2. Variables de entorno (opcionales; si no existen se usa localhost, usuario `postgres`, clave `postgres`):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/mesacero"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "tu_clave"
```

3. Compilar: `mvn clean package` → genera `target/mesacero.war`.
4. Copiar el WAR a `webapps/` de **Tomcat 10.1** e iniciarlo. La API queda en `http://localhost:8080/mesacero/api/...`.

> Si la profesora usa Tomcat 9 (o GlassFish antiguo) el paquete es `javax.servlet` en vez de `jakarta.servlet`:
> cambiar la dependencia del `pom.xml` a `javax.servlet:javax.servlet-api:4.0.1` y reemplazar los imports `jakarta.servlet` por `javax.servlet` en los servlets y el filtro.

## Pruebas rápidas (PowerShell, `curl.exe`)

```powershell
curl.exe http://localhost:8080/mesacero/api/mesas
curl.exe -X POST http://localhost:8080/mesacero/api/mesas -H "Content-Type: application/json" -d '{\"numero\":7,\"capacidad\":4}'
curl.exe -X PUT http://localhost:8080/mesacero/api/mesas/7 -H "Content-Type: application/json" -d '{\"numero\":7,\"capacidad\":8,\"estado\":\"disponible\"}'
curl.exe -X PATCH http://localhost:8080/mesacero/api/mesas/7 -H "Content-Type: application/json" -d '{\"estado\":\"ocupada\"}'
curl.exe -X DELETE http://localhost:8080/mesacero/api/mesas/7

curl.exe -X POST http://localhost:8080/mesacero/api/comandas -H "Content-Type: application/json" -d '{\"mesaId\":1,\"items\":[{\"productoId\":1,\"cantidad\":2}]}'
curl.exe -X PATCH http://localhost:8080/mesacero/api/comandas/1 -H "Content-Type: application/json" -d '{\"estado\":\"completada\"}'
```

## Códigos de respuesta

200 OK · 201 creado · 204 eliminado · 400 datos inválidos · 404 no existe · 405 método no permitido · 409 duplicado o con relaciones · 500 error del servidor.
