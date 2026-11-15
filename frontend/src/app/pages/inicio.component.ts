import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RestauranteService } from '../services/restaurante.service';

@Component({ selector: 'app-inicio', standalone: true, imports: [RouterLink], template: `
  <section class="page-heading"><div><span class="eyebrow">VISTA GENERAL</span><h1>Bienvenido a Mesa Cero</h1><p>Un espacio de trabajo para gestionar tu restaurante.</p></div><a class="button" routerLink="/comandas">Nueva comanda →</a></section>
  <div class="stats"><article class="stat"><span>Mesas totales</span><strong>{{s.mesas().length}}</strong><small>En el restaurante</small></article><article class="stat"><span>Mesas disponibles</span><strong>{{disponibles()}}</strong><small>Listas para atender</small></article><article class="stat"><span>Mesas ocupadas</span><strong>{{ocupadas()}}</strong><small>En servicio</small></article><article class="stat"><span>Comandas creadas</span><strong>{{s.comandas().length}}</strong><small>Durante esta sesión</small></article></div>
  <section class="panel"><div class="section-head"><div><span class="eyebrow">ACCESOS RÁPIDOS</span><h2>¿Qué deseas hacer?</h2></div></div><div class="quick-grid"><a routerLink="/mesas"><strong>Administrar mesas</strong><span>Disponibilidad y ocupación</span><b>Explorar →</b></a><a routerLink="/carta"><strong>Consultar carta</strong><span>Platillos, bebidas y precios</span><b>Explorar →</b></a><a routerLink="/comandas"><strong>Registrar comanda</strong><span>Simular una orden para cocina</span><b>Explorar →</b></a></div></section>
  <p class="notice">Este prototipo utiliza datos temporales en memoria. Los cambios se reinician al actualizar la página y todavía no se guardan en PostgreSQL.</p>` })
export class InicioComponent {
  readonly s = inject(RestauranteService);
  readonly disponibles = computed(() => this.s.mesas().filter(m => m.estado === 'disponible').length);
  readonly ocupadas = computed(() => this.s.mesas().filter(m => m.estado === 'ocupada').length);
}
