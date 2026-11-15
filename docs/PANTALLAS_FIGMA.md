# Correspondencia entre Figma (página 15) y Angular

El proyecto toma **como fuente de diseño el tema oscuro** de la página 15. El tema claro se deriva de las variables CSS en `src/styles.css`; no son dos copias separadas de las pantallas. La adaptación a tablet y escritorio también se genera mediante reglas responsive.

## Referencias de alta fidelidad consultadas

| Pantalla original | Nodo de Figma | Ruta de Angular |
| --- | --- | --- |
| Cliente — Bienvenida | `96:332` | `/bienvenida` |
| Cliente — Inicio / Cascada | `96:360` | `/inicio` |
| Cliente — Carta | `96:423` | `/carta` |
| Cliente — Carrito | `96:630` | `/carrito` |
| Cliente — Reserva | `96:685` | `/reserva` |
| Cliente — Acceso | `96:723` | `/acceso` |
| Cliente — Mapa | `96:776` | `/mapa` |
| Mesero — Mesas | `96:995` | `/personal/mesas` |
| Mesero — Pedido | `96:1039` | `/personal/pedido` |
| Admin — Dashboard | `96:1163` | `/admin/resumen` |
| Admin — Productos | `96:1326` | `/admin/productos` |
| Detalle — Filete al ajo | `117:479` | `/detalle/5` |
| Detalle — Cheesecake | `117:528` | `/detalle/6` |
| Error — 404 | `168:331` | `/error/404` |
| Recuperar acceso | `119:331` | `/recuperar` |

Las demás rutas utilizan patrones compartidos del mismo sistema gráfico (cabecera, chips, tarjetas, navegación, formularios, paneles) y requieren una validación individual con sus respectivos frames antes de dar por terminada una reproducción exacta. La página 15 contiene también estados alternativos, duplicados claros, variaciones de cantidad, y pantallas destino que el prototipo representa mediante las interacciones.

## Lista de revisión recomendada antes de entregar

- [ ] Comparar 390 × 844 px (móvil) con los frames oscuros de Figma.
- [ ] Comprobar 768 px (tablet), 1024 px y 1440 px (escritorio).
- [ ] Revisar que texto, botones, tablas y tarjetas no se recorten.
- [ ] Verificar navegación con clic y teclado.
- [ ] Probar filtro de carta, cantidades, reservación, selección de mesa y comanda.
- [ ] Alternar tema en todas las secciones, incluidas las administrativas.
- [ ] Revisar los estados de error y formularios de recuperación.
- [ ] Confirmar que las imágenes locales cargan sin recurrir a Figma.
- [ ] Sustituir datos simulados y acciones de autenticación/cobro al integrar Servlets y PostgreSQL.

## Cómo continuar sin consumir consultas innecesarias

1. Trabajar primero con las pantallas de este listado y comparar visualmente.
2. Consultar en Figma solo los frames que presenten diferencias o tengan componentes únicos.
3. Modificar un componente o variable compartida para corregir todas las vistas relacionadas.
4. Añadir nuevas pantallas como rutas Angular sin duplicar tema oscuro y claro.
