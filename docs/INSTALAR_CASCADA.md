# Instalar la cascada infinita de Selección del chef

Este complemento se probó con Angular 19 y el proyecto MesaCero-Inicio de la página 15 de Figma.

## Paso 1. Copiar los archivos nuevos

En `frontend/src/app/components/cascada/` copia:

- `cascada.component.ts`
- `cascada.component.css`

Si no existen las carpetas `components` y `cascada`, créalas.

## Paso 2. Abrir `frontend/src/app/pages/cliente.component.ts`

Debajo de los imports existentes, agrega:

```typescript
import { CascadaComponent } from '../components/cascada/cascada.component';
```

En el decorador `@Component`, agrega `CascadaComponent` a `imports`:

```typescript
imports: [CurrencyPipe, FormsModule, RouterLink, CascadaComponent]
```

Busca `class="featured-grid"`. Reemplaza **solo ese contenedor completo**, desde `<div class="featured-grid">` hasta su `</div>` correspondiente, por:

```html
<app-cascada [productos]="productosFiltrados()"></app-cascada>
```

Conserva los títulos `Selección del chef` y `Dos columnas · desliza y descubre` que están antes del contenedor.

## Paso 3. Probar

Desde la carpeta `frontend`:

```powershell
npm.cmd run build
npm.cmd start
```

Prueba `http://localhost:4200/inicio`. La cascada se repite, las dos columnas tienen un desfase, los botones permiten pausa e inversión y el icono `+` continúa agregando al carrito.

## Ajustar velocidades y tamaños

En `cascada.component.ts` encuentra:

```typescript
[style.--duracion]="(i === 0 ? 24 : 28) + 's'"
```

Cambiar `24` o `28` cambia la duración de cada columna. Menor número significa movimiento más rápido.

En `cascada.component.css` puedes ajustar `height: 455px` para mostrar más o menos productos y `padding-top: 70px` para aumentar o disminuir el desfase de la columna derecha.
