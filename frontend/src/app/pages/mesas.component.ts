import { Component, inject } from '@angular/core';
import { RestauranteService } from '../services/restaurante.service';
import { EstadoMesa } from '../models/restaurant.models';

@Component({ selector: 'app-mesas', standalone: true, template: `
  <section class="page-heading"><div><span class="eyebrow">OPERACIÓN</span><h1>Gestión de mesas</h1><p>Consulta y modifica el estado de las mesas.</p></div></section>
  <section class="panel"><div class="section-head"><h2>Mapa de mesas</h2><span class="muted">{{s.mesas().length}} mesas registradas</span></div><div class="table-grid">@for (mesa of s.mesas(); track mesa.id) {<article class="table-card"><div class="table-top"><span class="table-symbol">{{mesa.numero}}</span><span class="tag" [class]="mesa.estado">{{mesa.estado}}</span></div><h3>Mesa {{mesa.numero}}</h3><p>{{mesa.capacidad}} personas</p><label [for]="'estado-'+mesa.id">Cambiar estado</label><select [id]="'estado-'+mesa.id" [value]="mesa.estado" (change)="cambiar(mesa.id,$event)"><option value="disponible">Disponible</option><option value="ocupada">Ocupada</option><option value="reservada">Reservada</option></select></article>}</div></section>` })
export class MesasComponent {
  readonly s = inject(RestauranteService);
  cambiar(id: number, event: Event): void { this.s.cambiarEstadoMesa(id, (event.target as HTMLSelectElement).value as EstadoMesa); }
}
