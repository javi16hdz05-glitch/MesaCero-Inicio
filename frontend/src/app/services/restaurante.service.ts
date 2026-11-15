import { Injectable, signal } from '@angular/core';
import { Comanda, EstadoMesa, Mesa, Producto, ItemComanda } from '../models/restaurant.models';

/** Servicio de demostración. Sustituir internamente por HttpClient cuando exista la API Java. */
@Injectable({ providedIn: 'root' })
export class RestauranteService {
  readonly mesas = signal<Mesa[]>([
    { id: 1, numero: 1, capacidad: 2, estado: 'disponible' },
    { id: 2, numero: 2, capacidad: 4, estado: 'ocupada' },
    { id: 3, numero: 3, capacidad: 4, estado: 'disponible' },
    { id: 4, numero: 4, capacidad: 6, estado: 'reservada' },
    { id: 5, numero: 5, capacidad: 2, estado: 'disponible' },
    { id: 6, numero: 6, capacidad: 8, estado: 'disponible' }
  ]);

  readonly productos = signal<Producto[]>([
    { id: 1, nombre: 'Tlayuda', categoria: 'Platillos', precio: 120, disponible: true },
    { id: 2, nombre: 'Mole negro', categoria: 'Platillos', precio: 145, disponible: true },
    { id: 3, nombre: 'Enmoladas', categoria: 'Platillos', precio: 110, disponible: true },
    { id: 4, nombre: 'Agua de horchata', categoria: 'Bebidas', precio: 35, disponible: true },
    { id: 5, nombre: 'Café de olla', categoria: 'Bebidas', precio: 40, disponible: true },
    { id: 6, nombre: 'Flan', categoria: 'Postres', precio: 55, disponible: false }
  ]);

  readonly comandas = signal<Comanda[]>([]);

  cambiarEstadoMesa(id: number, estado: EstadoMesa): void {
    this.mesas.update(mesas => mesas.map(m => m.id === id ? { ...m, estado } : m));
  }

  crearComanda(mesaId: number, items: ItemComanda[]): Comanda {
    const productos = this.productos();
    if (!this.mesas().some(m => m.id === mesaId)) throw new Error('Mesa no válida');
    if (!items.length || items.some(i => i.cantidad <= 0 || !productos.some(p => p.id === i.productoId && p.disponible))) {
      throw new Error('La comanda debe tener productos disponibles');
    }
    const total = items.reduce((s, item) => s + (productos.find(p => p.id === item.productoId)?.precio ?? 0) * item.cantidad, 0);
    const comanda: Comanda = { id: Date.now(), mesaId, items: items.map(i => ({ ...i })), total, fecha: new Date().toISOString(), estado: 'pendiente' };
    this.comandas.update(lista => [...lista, comanda]);
    this.cambiarEstadoMesa(mesaId, 'ocupada');
    return comanda;
  }
}
