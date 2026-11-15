import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RestauranteService } from '../services/restaurante.service';

@Component({ selector: 'app-comandas', standalone: true, imports: [CurrencyPipe], template: `
  <section class="page-heading"><div><span class="eyebrow">COCINA</span><h1>Nueva comanda</h1><p>Elabora una orden de prueba y consulta el resumen.</p></div></section>
  <div class="two-column"><section class="panel"><h2>Selecciona los productos</h2><label for="mesa">Mesa de destino</label><select id="mesa" [value]="mesaId()" (change)="mesaId.set(+$any($event.target).value)">@for (mesa of s.mesas(); track mesa.id) {<option [value]="mesa.id">Mesa {{mesa.numero}} · {{mesa.capacidad}} personas</option>}</select><div class="order-list">@for (producto of s.productos(); track producto.id) {@if (producto.disponible) {<div class="order-item"><div><strong>{{producto.nombre}}</strong><small>{{producto.precio | currency:'MXN'}}</small></div><div class="stepper"><button (click)="cambiar(producto.id,-1)" [disabled]="cantidad(producto.id)===0" [attr.aria-label]="'Quitar '+producto.nombre">−</button><span>{{cantidad(producto.id)}}</span><button (click)="cambiar(producto.id,1)" [attr.aria-label]="'Agregar '+producto.nombre">+</button></div></div>}}</div></section><aside class="panel summary"><span class="eyebrow">RESUMEN</span><h2>Tu orden</h2><p class="muted">{{cantidadTotal()}} artículos seleccionados</p><div class="total-row"><span>Total</span><strong>{{total() | currency:'MXN'}}</strong></div><button class="button full" [disabled]="cantidadTotal()===0" (click)="guardar()">Registrar comanda</button>@if (mensaje()) {<p class="success-message" role="status">{{mensaje()}}</p>}<p class="muted small">Registro temporal: no se envía a ningún servidor.</p></aside></div>
  <section class="panel history"><h2>Comandas de esta sesión</h2>@for (comanda of s.comandas(); track comanda.id) {<div class="history-row"><span>Mesa {{comanda.mesaId}} · {{comanda.items.length}} productos</span><strong>{{comanda.total | currency:'MXN'}}</strong></div>} @empty {<p class="muted">Todavía no hay comandas registradas.</p>}</section>` })
export class ComandasComponent {
  readonly s = inject(RestauranteService);
  readonly mesaId = signal(1);
  readonly cantidades = signal<Record<number, number>>({});
  readonly mensaje = signal('');
  readonly cantidadTotal = computed(() => Object.values(this.cantidades()).reduce((a,b) => a+b,0));
  readonly total = computed(() => this.s.productos().reduce((a,p) => a+p.precio*(this.cantidades()[p.id] || 0),0));
  cantidad(id: number): number { return this.cantidades()[id] || 0; }
  cambiar(id: number, delta: number): void { this.cantidades.update(v => ({...v, [id]: Math.max(0,(v[id] || 0)+delta)})); this.mensaje.set(''); }
  guardar(): void {
    const items = Object.entries(this.cantidades()).filter(([,n]) => n>0).map(([id,n])=>({productoId:+id,cantidad:n}));
    if (!items.length) return;
    this.s.crearComanda(this.mesaId(), items);
    this.cantidades.set({});
    this.mensaje.set('Comanda registrada correctamente en esta sesión.');
  }
}
