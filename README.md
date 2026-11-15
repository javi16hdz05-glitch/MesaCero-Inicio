# Mesa Cero — Angular · Diseño de Figma

Versión visual del sistema Mesa Cero para la materia de Programación Web, basada en la **página 15** del archivo [MesaCero – Wireframes completos](https://www.figma.com/design/EXC1XNJzy2SUPA2I1kmG3y/MesaCero-%E2%80%93-Wireframes-completos?node-id=96-331).

## Requisitos y ejecución (Windows)

- Node.js 22 y npm.
- Visual Studio Code.
- GitHub Desktop para sincronizar el repositorio.

Abre la carpeta `MesaCero-Inicio` en VS Code. En la terminal PowerShell:

```powershell
cd frontend
npm.cmd ci
npm.cmd start
```

Luego entra a **http://localhost:4200/**. Para detener Angular, presiona `Ctrl+C` en la terminal.

## Qué incluye esta entrega

- Navegación de cliente: bienvenida, inicio con selección del chef, carta, bebidas, detalle dinámico de platillos, carrito, confirmación de pedido, reservación, selección de mesa, acceso, registro, perfil e historiales.
- Navegación de personal: acceso, mesas, pedido, cocina, cobro y cierre.
- Administración: dashboard, mesas, productos, reservaciones y ventas.
- Recuperación de acceso y 12 estados de error/acceso, disponibles como rutas de demostración.
- Imágenes originales de los platillos de Figma almacenadas localmente en `frontend/public/assets`.
- **Dos temas generados desde código:** oscuro basado en el diseño de Figma y claro con la misma jerarquía y lenguaje visual.
- Adaptación para móvil, tablet y computadora mediante CSS Grid, Flexbox y media queries.

**Esta es una primera implementación integral de las rutas y patrones visuales, no una auditoría píxel por píxel de cada estado del archivo de Figma.** Algunas pantallas de Figma representan variantes de un mismo flujo, que aquí se resuelven mediante estado e interacciones. Valida cada pantalla importante contra el diseño original antes de entregarla a la profesora.

## Navegación rápida

| Sección | Ruta |
| --- | --- |
| Bienvenida | `/bienvenida` |
| Inicio cliente | `/inicio` |
| Carta | `/carta` |
| Reservar | `/reserva` |
| Carrito | `/carrito` |
| Acceso mesero | `/personal/acceso` |
| Mesas de mesero | `/personal/mesas` |
| Dashboard admin | `/admin/resumen` |
| Productos admin | `/admin/productos` |
| Error 404 | `/error/404` |

## Cómo se organiza el proyecto

```text
MesaCero-Inicio/
├── frontend/
│   ├── public/assets/                  # Fotos e iconos locales de Figma
│   ├── src/app/
│   │   ├── pages/                      # Cliente, personal, admin y estados
│   │   ├── models/                     # Tipos compartidos de TypeScript
│   │   ├── services/
│   │   │   ├── restaurante.service.ts  # Datos y acciones simuladas
│   │   │   └── tema.service.ts         # Cambio claro/oscuro
│   │   ├── app.routes.ts               # Rutas del sistema
│   │   └── app.component.ts            # Cabeceras y navegación adaptable
│   └── src/styles.css                  # Variables de tema y responsive
├── docs/PLAN_DE_INTEGRACION.md
└── README.md
```

## Importante sobre los datos

Las interacciones actualmente usan datos **ficticios y temporales en memoria**: desaparecen al actualizar la página. Inicio de sesión, envío de códigos, cobros y gráficas administrativas **no son operaciones reales**. Aún no hay servidor, Servlets ni PostgreSQL. No usar como aplicación de producción ni ingresar contraseñas verdaderas.

## Actualizar tu repositorio de GitHub Desktop

Si ya tienes `MesaCero-Inicio` publicado, **no crees otro repositorio**:

1. Descarga el nuevo ZIP.
2. Descomprímelo en una carpeta temporal.
3. Copia el contenido de `MesaCero-Inicio` del ZIP dentro de la carpeta de tu repositorio local, reemplazando archivos cuando Windows lo solicite. **Conserva la carpeta oculta `.git` del repositorio**; no la sustituyas ni elimines.
4. Abre GitHub Desktop: aparecerán los cambios.
5. Revisa la lista de archivos, escribe un resumen como `feat: integrar diseño de Figma página 15` y realiza **Commit to main** (o una rama si estás trabajando en equipo).
6. Presiona **Push origin**.
7. Desde VS Code ejecuta `npm.cmd ci` y `npm.cmd start` para probar localmente.

**Recomendación de equipo:** realizar cambios de nuevas pantallas en ramas y fusionarlos mediante Pull Requests.

## Compilar para verificar

```powershell
cd frontend
npm.cmd run build
```

## Próximas integraciones

Consulta [docs/PLAN_DE_INTEGRACION.md](docs/PLAN_DE_INTEGRACION.md) para añadir Java Servlets, API REST y PostgreSQL sin cambiar la interfaz. La [tabla de correspondencia con Figma](docs/PANTALLAS_FIGMA.md) documenta qué vistas se estudiaron en detalle y qué falta contrastar.
