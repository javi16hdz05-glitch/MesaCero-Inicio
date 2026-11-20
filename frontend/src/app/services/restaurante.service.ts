import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Comanda, EstadoMesa, Mesa, Producto, Reserva, ItemComanda } from '../models/restaurant.models';

/** Ruta base de la API. En desarrollo la redirige proxy.conf.json hacia Tomcat. */
const API = '/api';

/**
 * Datos del restaurante. Al iniciar se cargan desde los Servlets (mesas, productos,
 * comandas y reservas). Los valores de abajo son solo respaldo para que la interfaz
 * siga funcionando si el backend no está encendido.
 */
@Injectable({ providedIn: 'root' })
export class RestauranteService {
  private readonly http = inject(HttpClient);
  /** true cuando la última llamada al backend funcionó. */
  readonly apiDisponible = signal(false);

  readonly mesas = signal<Mesa[]>([
    {id:1,numero:1,capacidad:2,estado:'disponible'}, {id:2,numero:2,capacidad:4,estado:'ocupada'},
    {id:3,numero:3,capacidad:4,estado:'disponible'}, {id:4,numero:4,capacidad:6,estado:'disponible'},
    {id:5,numero:5,capacidad:2,estado:'disponible'}, {id:6,numero:6,capacidad:6,estado:'ocupada'}
  ]);
  readonly productos = signal<Producto[]>([
    {id:1,nombre:'Tlayuda especial',categoria:'Oaxaqueños',precio:185,disponible:true,descripcion:'Tasajo, quesillo, frijol negro y aguacate.',foto:'assets/tlayuda.png',etiqueta:'Popular'},
    {id:2,nombre:'Salmón cítrico',categoria:'Fuertes',precio:245,disponible:true,descripcion:'Salmón asado con cítricos frescos.',foto:'assets/salmon-raw.png',etiqueta:'Nuevo'},
    {id:3,nombre:'Pasta de la casa',categoria:'Fuertes',precio:198,disponible:true,descripcion:'Pasta artesanal con salsa y hierbas.',foto:'assets/pasta.png'},
    {id:4,nombre:'Enchiladas oaxaqueñas',categoria:'Oaxaqueños',precio:165,disponible:true,descripcion:'Pollo, salsa de chile y queso.',foto:'assets/enchiladas.png'},
    {id:5,nombre:'Filete al ajo',categoria:'Fuertes',precio:295,disponible:true,descripcion:'Corte a la parrilla con puré.',foto:'assets/filete.png'},
    {id:6,nombre:'Cheesecake de frutos rojos',categoria:'Postres',precio:135,disponible:true,descripcion:'Postre cremoso y frutos frescos.',foto:'assets/cheesecake.png'},
    {id:7,nombre:'Agua de jamaica',categoria:'Bebidas',precio:55,disponible:true,descripcion:'Refrescante y natural.',foto:'assets/jamaica.png'}
  ]);
  readonly carrito = signal<ItemComanda[]>([]);
  readonly totalCarrito = computed(() => this.carrito().reduce((t,i)=>t+(this.productos().find(p=>p.id===i.productoId)?.precio||0)*i.cantidad,0));
  readonly cantidadCarrito = computed(()=>this.carrito().reduce((t,i)=>t+i.cantidad,0));
  readonly comandas = signal<Comanda[]>([]);
  readonly reservas = signal<Reserva[]>([]);
  readonly reservaPendiente = signal({fecha:'',hora:'20:30',personas:2,zona:'Salón interior / Terraza'});
  readonly mesaElegida = signal<number|null>(null);

  constructor() { this.cargarTodo(); }

  /** Trae todo de la base de datos. Si el backend no responde, se quedan los datos de respaldo. */
  cargarTodo(): void {
    this.http.get<Mesa[]>(`${API}/mesas`).subscribe({
      next: m => { this.mesas.set(m); this.apiDisponible.set(true); },
      error: () => this.apiNoDisponible()
    });
    this.http.get<Producto[]>(`${API}/productos`).subscribe({
      next: p => this.productos.set(p),
      error: () => this.apiNoDisponible()
    });
    this.http.get<Comanda[]>(`${API}/comandas`).subscribe({
      next: c => this.comandas.set(c),
      error: () => this.apiNoDisponible()
    });
    this.http.get<Reserva[]>(`${API}/reservas`).subscribe({
      next: r => this.reservas.set(r),
      error: () => this.apiNoDisponible()
    });
  }

  private apiNoDisponible(): void {
    this.apiDisponible.set(false);
    console.warn('No se pudo conectar con el backend (/api). Se usan los datos de respaldo.');
  }

  cantidad(productoId:number):number {return this.carrito().find(i=>i.productoId===productoId)?.cantidad||0;}
  cambiarCantidad(productoId:number, delta:number):void {
    const p=this.productos().find(x=>x.id===productoId);
    if(!p?.disponible)return;
    this.carrito.update(items=>{
      const cantidad=Math.max(0,(items.find(i=>i.productoId===productoId)?.cantidad||0)+delta);
      return [...items.filter(i=>i.productoId!==productoId),...(cantidad?[{productoId,cantidad}]:[])];
    });
  }
  private marcarMesa(id:number,estado:EstadoMesa):void {this.mesas.update(m=>m.map(x=>x.id===id?{...x,estado}:x));}

  /** PATCH /api/mesas/{id} */
  cambiarEstadoMesa(id:number,estado:EstadoMesa):void {
    this.marcarMesa(id,estado);
    this.http.patch<Mesa>(`${API}/mesas/${id}`,{estado}).subscribe({error:()=>this.cargarTodo()});
  }
  crearComanda(mesaId:number,items:ItemComanda[]):Comanda {
    if(!items.length||!this.mesas().some(m=>m.id===mesaId))throw new Error('Comanda inválida');
    const total=items.reduce((t,i)=>t+(this.productos().find(p=>p.id===i.productoId)?.precio||0)*i.cantidad,0);
    const orden:Comanda={id:Date.now(),mesaId,items:items.map(i=>({...i})),total,fecha:new Date().toISOString(),estado:'en_preparacion'};
    this.comandas.update(c=>[orden,...c]);this.marcarMesa(mesaId,'ocupada');
    // POST /api/comandas: el servidor calcula el total real y guarda todo en PostgreSQL
    this.http.post<Comanda>(`${API}/comandas`,{mesaId,items}).subscribe({
      next:()=>this.cargarTodo(),
      error:()=>this.cargarTodo()
    });
    return orden;
  }
  confirmarPedido():Comanda {const items=this.carrito(); const mesa=this.mesaElegida();if(!mesa)throw new Error('Selecciona una mesa');const orden=this.crearComanda(mesa,items);this.carrito.set([]);return orden;}
  guardarReserva():Reserva {
    const d=this.reservaPendiente(),mesa=this.mesaElegida();
    if(!mesa||!d.fecha)throw new Error('Selecciona una fecha y mesa');
    const r:Reserva={id:Date.now(),mesaId:mesa,fecha:d.fecha,hora:d.hora,personas:d.personas,zona:d.zona};
    this.reservas.update(rs=>[r,...rs]);this.marcarMesa(mesa,'reservada');
    // POST /api/reservas
    this.http.post<Reserva>(`${API}/reservas`,{mesaId:mesa,fecha:d.fecha,hora:d.hora,personas:d.personas,zona:d.zona}).subscribe({
      next:()=>this.cargarTodo(),
      error:()=>this.cargarTodo()
    });
    return r;
  }
}
