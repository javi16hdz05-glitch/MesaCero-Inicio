# Mesa Cero — Inicio Angular

Proyecto académico nuevo e independiente para Programación Web. Primera etapa: interfaz de restaurante con Angular 19, TypeScript, HTML y CSS. Preparado para incorporar Java, Servlets, JDBC y PostgreSQL más adelante.

## Requisitos

- Git.
- Node.js 22 LTS compatible con Angular 19 y npm.
- Visual Studio Code (recomendado).

## Ejecutar en Windows (PowerShell)

```powershell
cd frontend
npm ci
npm start
```

Abrir http://localhost:4200.

## Funciones disponibles

- Inicio: indicadores y navegación.
- Mesas: listar y cambiar estados.
- Carta: consultar y filtrar productos.
- Comandas: elegir mesa, añadir productos, calcular total y registrar en memoria.
- Diseño adaptable a computadora y teléfono.

**Importante:** los datos son ficticios y viven solo en memoria: desaparecen al recargar. La aplicación no utiliza todavía un backend, autenticación ni PostgreSQL.

## Estructura

```text
MesaCero-Inicio/
├── frontend/
│   └── src/app/
│       ├── pages/       # Pantallas con rutas Angular
│       ├── models/      # Tipos de datos compartidos
│       ├── services/    # Lógica y datos de demostración
│       ├── app.routes.ts
│       └── app.component.ts
├── docs/                 # Guías de crecimiento
├── .gitignore
└── README.md
```

## Publicarlo en un repositorio NUEVO de GitHub

1. Crear un repositorio vacío en GitHub con nombre `MesaCero-Inicio`, sin README ni .gitignore automáticos.
2. Abrir PowerShell en la carpeta `MesaCero-Inicio` y ejecutar:

```powershell
git init
git add .
git commit -m "feat: iniciar proyecto Angular de Mesa Cero"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/MesaCero-Inicio.git
git push -u origin main
```

Sustituir `TU_USUARIO` por tu cuenta. Para colaborar, invitar a los compañeros desde Settings > Collaborators. Trabajar con ramas y pull requests.

## Compilar

```powershell
cd frontend
npm run build
```

## Próxima etapa

Consultar [Plan de integración](docs/PLAN_DE_INTEGRACION.md).
