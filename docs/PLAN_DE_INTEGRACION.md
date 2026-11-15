# Plan de crecimiento — Mesa Cero

## Etapa 1 — Angular (actual)

Terminar pantallas y componentes visuales; navegación Angular Router; formularios, filtros, validaciones y servicios de demostración. Los datos actuales son volátiles.

## Etapa 2 — Formularios y estados

Agregar clientes, reservaciones y pagos cuando el alcance académico esté definido. Utilizar Reactive Forms para formularios complejos. Acordar contratos de datos con el equipo.

## Etapa 3 — Java y Servlets

Incorporar `backend/` con un proyecto Maven WAR para Jakarta Servlet y el servidor de aplicaciones indicado por la profesora. Implementar controladores Servlet, servicios y clases DAO. Nunca conectar Angular directamente a PostgreSQL.

Endpoints REST propuestos (acordarlos antes de programar):

| Método | Ruta | Función |
| --- | --- | --- |
| GET | `/api/mesas` | Obtener mesas |
| PATCH | `/api/mesas/{id}` | Modificar estado |
| GET | `/api/productos` | Obtener carta |
| POST | `/api/comandas` | Registrar comanda |
| GET | `/api/comandas` | Consultar comandas |

Después, sustituir gradualmente el origen de datos de `RestauranteService` por llamadas `HttpClient`. Separar servicios por dominio a medida que crezca la aplicación. Configurar un proxy de Angular para desarrollo local.

## Etapa 4 — PostgreSQL

Crear scripts SQL versionados; definir restricciones, claves foráneas y pruebas. Conectar el backend mediante JDBC y DAO; configurar host, puerto, base, usuario y contraseña con variables de entorno. **Nunca subir contraseñas ni `.env` reales.**

## Etapa 5 — Seguridad, nube y pruebas

Agregar autorización por roles, validación en servidor, manejo de errores, CORS restringido, pruebas de integración y documentación de servicios. Evaluar despliegue cuando la profesora lo solicite.

## Flujo de trabajo en equipo

```powershell
git switch main
git pull origin main
git switch -c feature/nombre-funcion
# Editar archivos, probar y revisar cambios
git add .
git commit -m "feat: describir la funcion"
git push -u origin feature/nombre-funcion
```

Abrir un Pull Request para integrar a `main`. No subir `node_modules`, archivos compilados ni secretos.
