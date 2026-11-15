import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { RestauranteService } from '../services/restaurante.service';

@Component({selector:'app-admin',standalone:true,imports:[CurrencyPipe,RouterLink],template:`
  <div class="admin-page"><div class="admin-eyebrow">MESA CERO / DIRECCIÓN <span>OAXACA · 2026</span></div>
  <div class="admin-heading"><h1>{{titulo}}</h1><p>{{subtitulo}}</p></div>
  <div class="admin-stats">@for(k of indicadores;track k.label){<div class="glass stat-card"><span>{{k.label}}</span><strong>{{k.value}}</strong><small>{{k.note}}</small></div>}</div>
  @if(page==='resumen'){
    <div class="admin-charts"><section class="glass chart-panel"><h2>Ventas por horario</h2><p>Comparativa de actividad por franja horaria</p><div class="bars" role="img" aria-label="Gráfica demostrativa de ventas por hora">@for(v of barras;track $index){<div><i [style.height.%]="v" [class.gold-bar]="$index===7"></i><span>{{10+$index}}</span></div>}</div></section><section class="glass insights"><h2>Lecturas para decidir</h2><p>● La franja 17–18 h concentra más ventas.</p><p>● Las mesas de 2 personas tienen alta demanda.</p><p>● Sugerencia: reforzar personal de cocina.</p><a routerLink="/admin/ventas" class="btn secondary">Explorar ventas</a></section></div>
    <section class="glass admin-table"><h2>Últimas órdenes</h2><div class="admin-row admin-row-head"><span>ORDEN</span><span>MESA</span><span>TOTAL</span><span>ESTADO</span></div>@for(o of datos.comandas().slice(0,5);track o.id){<div class="admin-row"><strong>#ORD-{{o.id.toString().slice(-4)}}</strong><span>M{{o.mesaId.toString().padStart(2,'0')}}</span><span>{{o.total|currency:'MXN'}}</span><em>{{o.estado}}</em></div>}@empty{<p class="muted">No hay órdenes registradas en esta sesión. Las estadísticas superiores son de ejemplo.</p>}</section>
  }@else if(page==='mesas'){
    <section class="glass admin-table"><h2>Gestión de mesas</h2><div class="admin-row admin-row-head"><span>MESA</span><span>CAPACIDAD</span><span>ESTADO</span><span>ACCIÓN</span></div>@for(m of datos.mesas();track m.id){<div class="admin-row"><strong>M{{m.numero.toString().padStart(2,'0')}}</strong><span>{{m.capacidad}} personas</span><span>{{m.estado}}</span><select [value]="m.estado" (change)="cambiarMesa(m.id,$event)"><option value="disponible">Disponible</option><option value="ocupada">Ocupada</option><option value="reservada">Reservada</option></select></div>}</section>
  }@else if(page==='productos'){
    <section class="glass admin-table"><div class="admin-row admin-row-head"><span>PRODUCTO</span><span>CATEGORÍA</span><span>PRECIO</span><span>ESTADO</span></div>@for(p of datos.productos();track p.id){<div class="admin-row"><strong>{{p.nombre}}</strong><span>{{p.categoria}}</span><span>{{p.precio|currency:'MXN'}}</span><em>{{p.disponible?'Disponible':'Agotado'}}</em></div>}</section><section class="glass admin-callout"><h2>Catálogo listo para mantener al día</h2><a routerLink="/admin/ventas" class="btn secondary">Ver ventas por platillo</a></section>
  }@else if(page==='reservas'){
    <section class="glass admin-table"><div class="admin-row admin-row-head"><span>RESERVA</span><span>MESA</span><span>FECHA</span><span>PERSONAS</span></div>@for(r of datos.reservas();track r.id){<div class="admin-row"><strong>#{{r.id.toString().slice(-4)}}</strong><span>M{{r.mesaId}}</span><span>{{r.fecha}} {{r.hora}}</span><em>{{r.personas}}</em></div>}@empty{<p class="muted">Todavía no hay reservas en esta sesión.</p>}</section>
  }@else if(page==='ventas'){
    <div class="admin-charts"><section class="glass chart-panel"><h2>Ventas de demostración</h2><p>Datos ilustrativos para el diseño de reportes</p><div class="bars">@for(v of barras;track $index){<div><i [style.height.%]="v" [class.gold-bar]="$index===7"></i><span>{{10+$index}}</span></div>}</div></section><section class="glass insights"><h2>Totales de esta sesión</h2><strong class="grand-number">{{ventasReales()|currency:'MXN'}}</strong><p>Comandas registradas: {{datos.comandas().length}}</p><p>No se procesa ningún pago real.</p></section></div>
  }
  </div>
`})
export class AdminComponent {
  readonly page=inject(ActivatedRoute).snapshot.data['page'] as string;
  readonly datos=inject(RestauranteService);
  readonly barras=[23,34,47,29,62,82,71,100,65];
  readonly ventasReales=computed(()=>this.datos.comandas().reduce((a,o)=>a+o.total,0));
  get titulo():string{return ({resumen:'Dashboard',mesas:'Mesas',productos:'Productos',reservas:'Reservaciones',ventas:'Ventas'} as Record<string,string>)[this.page];}
  get subtitulo():string{return ({resumen:'Indicadores operativos en tiempo real',mesas:'Supervisa la ocupación y disponibilidad',productos:'Gestiona categorías, precios y disponibilidad',reservas:'Consulta las reservas del restaurante',ventas:'Indicadores de actividad del restaurante'} as Record<string,string>)[this.page];}
  get indicadores():{label:string;value:string;note:string}[]{
    if(this.page==='resumen')return [{label:'VENTAS DEL DÍA',value:'$18,540',note:'+12.8% vs ayer · muestra'},{label:'PEDIDOS',value:'82',note:'+7.2% vs ayer · muestra'},{label:'OCUPACIÓN',value:'76%',note:'+5.0% vs ayer · muestra'},{label:'TICKET PROM.',value:'$226',note:'+4.3% vs ayer · muestra'}];
    if(this.page==='productos')return [{label:'PRODUCTOS ACTIVOS',value:''+this.datos.productos().length,note:'Catálogo publicado'},{label:'CATEGORÍAS',value:'4',note:'Organización completa'},{label:'MÁS VENDIDO',value:'Tlayuda',note:'Producto destacado'},{label:'AGOTADOS',value:'0',note:'Revisar inventario'}];
    if(this.page==='mesas')return [{label:'MESAS TOTALES',value:''+this.datos.mesas().length,note:'Salón'},{label:'DISPONIBLES',value:''+this.datos.mesas().filter(x=>x.estado==='disponible').length,note:'Listas para atender'},{label:'OCUPADAS',value:''+this.datos.mesas().filter(x=>x.estado==='ocupada').length,note:'Con servicio'},{label:'RESERVADAS',value:''+this.datos.mesas().filter(x=>x.estado==='reservada').length,note:'Próximas llegadas'}];
    return [{label:'ÓRDENES',value:''+this.datos.comandas().length,note:'Esta sesión'},{label:'RESERVAS',value:''+this.datos.reservas().length,note:'Esta sesión'},{label:'VENTAS REGISTRADAS',value:'$'+this.ventasReales(),note:'Datos de prueba'},{label:'MESAS',value:''+this.datos.mesas().length,note:'Total disponible'}];
  }
  cambiarMesa(id:number,event:Event):void{this.datos.cambiarEstadoMesa(id,(event.target as HTMLSelectElement).value as 'disponible'|'ocupada'|'reservada');}
}
