# Conectar Angular 19 a los Servlets

**No sustituyas todavía `RestauranteService` ni borres las pantallas.** Esta carpeta contiene el puente para integrar por etapas, sin perder la estética Figma ni la cascada del chef.

## Paso 1. Archivos que sí debes copiar

1. Copia `backend/` al mismo nivel que `frontend/` de tu repositorio `MesaCero-Inicio`. Quedará `MesaCero-Inicio/backend` y `MesaCero-Inicio/frontend`.
2. Copia `integracion-angular/mesacero-api.service.ts` a `frontend/src/app/services/mesacero-api.service.ts`.
3. Copia `integracion-angular/proxy.conf.json` a `frontend/proxy.conf.json`.

El archivo `frontend/src/app/app.config.ts` **ya incorpora `provideHttpClient()`**, no hace falta duplicarlo.

## Paso 2. Ejecutar

Con PostgreSQL y Tomcat ejecutándose, desde `frontend`:

```powershell
npm.cmd start -- --proxy-config proxy.conf.json
```

En el navegador, entrar a `http://localhost:4200/inicio`.

## Paso 3. Incorporar las lecturas a RestauranteService

Al abrir el home, puedes cargar los platillos reales sin cambiar los componentes de diseño:

```ts
import { inject } from '@angular/core';
import { MesaCeroApiService } from './mesacero-api.service';

// Dentro de RestauranteService, NO fueras de la clase:
private readonly api = inject(MesaCeroApiService);

cargarCatalogo(): void {
  this.api.obtenerCatalogo().subscribe({
    next: (productos) => this.productos.set(productos),
    error: (err) => console.error('No se pudo cargar el catálogo', err)
  });
}

cargarMesas(): void {
  this.api.obtenerMesas().subscribe({
    next: (mesas) => this.mesas.set(mesas),
    error: (err) => console.error('No se pudo cargar el estado de mesas', err)
  });
}
```

**Invoca esas funciones en el flujo de inicialización** cuando estés listo (por ejemplo, en el constructor del servicio). Si el backend aún no está configurado, puedes mantener temporalmente los datos demo del servicio original.

## Paso 4. Reservas

En `mesacero-api.service.ts` ya existe `registrarReserva()`. Debes enviar una fecha con horario e información de contacto real, por ejemplo:

```ts
this.api.registrarReserva({
  mesaId: 1,
  nombreContacto: 'Cliente de prueba',
  telefonoContacto: '9511234567',
  personas: 2,
  inicio: '2026-12-01T20:30:00-06:00',
  fin: '2026-12-01T22:30:00-06:00'
}).subscribe({
  next: (reserva) => console.log('Reserva registrada:', reserva.id),
  error: (e) => console.error('Revisa capacidad, fecha u ocupación', e)
});
```

## Paso 5. Comandas y administradores

Por seguridad, los métodos bajo `/api/admin/*` **no se pueden llamar desde Angular con una contraseña permanente incrustada**. Hasta que implementemos login de usuarios con roles, prueba las acciones de meseros, administradores y cajeros con Postman/PowerShell local y la llave de demo `X-Admin-Key`.

Las pantallas actuales de acceso/registro y carrito siguen siendo una **simulación** hasta que conectemos autorización real. No marques como terminada la autenticación por tener únicamente el backend compilado.
