import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RestauranteService } from '../services/restaurante.service';

@Component({ selector: 'app-carta', standalone: true, imports: [CurrencyPipe], template: `
  <section class="page-heading"><div><span class="eyebrow">MENÚ</span><h1>Carta de productos</h1><p>Consulta el catálogo de platillos y bebidas.</p></div></section>
  <div class="filters">@for (categoria of categorias; track categoria) {<button [class.selected]="filtro()===categoria" (click)="filtro.set(categoria)">{{categoria}}</button>}</div>
  <section class="product-grid">@for (producto of s.productos(); track producto.id) {@if (filtro()==='Todos' || producto.categoria===filtro()) {<article class="product-card"><div class="product-art">✦</div><div class="product-content"><span class="eyebrow">{{producto.categoria}}</span><h3>{{producto.nombre}}</h3><div class="product-bottom"><strong>{{producto.precio | currency:'MXN':'symbol':'1.2-2'}}</strong><span class="tag" [class.agotado]="!producto.disponible">{{producto.disponible ? 'Disponible':'Agotado'}}</span></div></div></article>}}</section>` })
export class CartaComponent {
  readonly s = inject(RestauranteService);
  readonly categorias = ['Todos', 'Platillos', 'Bebidas', 'Postres'];
  readonly filtro = signal('Todos');
}
