import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RestauranteService } from '../services/restaurante.service';

@Component({selector:'app-personal',standalone:true,imports:[CommonModule,CurrencyPipe,FormsModule,RouterLink],template:`
  @if(page==='acceso'){
    <div class="page-heading"><span class="eyebrow">ÁREA DEL PERSONAL</span><h1>Acceso al salón</h1><p>Herramientas para atender cada mesa.</p></div><form class="glass form-panel" (ngSubmit)="entrar()"><label for="usuario">USUARIO</label><input id="usuario" name="usuario" [(ngModel)]="usuario" required placeholder="Usuario del personal"><label for="pass">CONTRASEÑA</label><input id="pass" name="pass" [(ngModel)]="pass" type="password" required placeholder="••••••••"><button class="btn primary">Entrar en modo demostración</button><p class="form-note">La autenticación real se integrará con Servlets y PostgreSQL.</p></form>
  }@else if(page==='mesas'){
    <div class="page-heading"><span class="eyebrow">GESTIÓN DE SALÓN</span><h1>Mesas asignadas</h1><p>Vista rápida para agilizar la atención.</p></div><div class="staff-card-list">@for(m of datos.mesas().slice(0,4);track m.id){<button class="staff-table glass" (click)="seleccionarMesa(m.id)"><span class="table-number" [class.occupied]="m.estado==='ocupada'">{{m.numero.toString().padStart(2,'0')}}</span><span class="staff-table-info"><strong>Mesa M{{m.numero.toString().padStart(2,'0')}}</strong><small [class.gold-text]="m.estado==='ocupada'">{{m.estado==='ocupada'?'Orden activa':'Disponible'}}</small></span><b>→</b></button>}</div><a routerLink="/personal/pedido" class="btn primary bottom-action">Crear orden sin cuenta</a>
  }@else if(page==='pedido'){
    <div class="page-heading"><span class="eyebrow">NUEVA COMANDA</span><h1>Orden · Mesa {{mesaId().toString().padStart(2,'0')}}</h1><p>El servicio continúa incluso con clientes invitados.</p></div>
    <div class="glass form-panel guest-card"><span class="eyebrow">CLIENTE DE MOSTRADOR</span><h3>Cliente genérico (sin cuenta)</h3><p>Mesero: turno actual</p></div>
    <div class="staff-card-list order-cards">@for(p of datos.productos().slice(0,5);track p.id){<div class="glass staff-order-line"><div><h3>{{p.nombre}}</h3><strong>{{p.precio|currency:'MXN':'symbol':'1.0-0'}}</strong></div><div class="inline-qty"><button (click)="cantidad(p.id,-1)" [attr.aria-label]="'Quitar '+p.nombre">−</button><span>{{cantidades()[p.id]||0}}</span><button (click)="cantidad(p.id,1)" [attr.aria-label]="'Agregar '+p.nombre">+</button></div></div>}</div>
    <button class="btn primary bottom-action" [disabled]="totalCantidad()===0" (click)="enviar()">Enviar comanda a cocina · {{total()|currency:'MXN'}}</button>@if(mensaje()){<p class="feedback" role="status">{{mensaje()}}</p>}
  }@else if(page==='cocina'){
    <div class="page-heading"><span class="eyebrow">PREPARACIÓN</span><h1>Comandas en cocina</h1><p>Órdenes que requieren atención.</p></div><div class="staff-card-list">@for(o of datos.comandas();track o.id){<div class="glass simple-record"><h3>Mesa {{o.mesaId.toString().padStart(2,'0')}}</h3><p>{{o.items.length}} productos · {{o.total|currency:'MXN'}}</p><span class="state-chip">{{o.estado}}</span></div>}@empty{<div class="glass empty-state"><h2>Sin comandas pendientes</h2><p>Las órdenes registradas aparecerán aquí.</p><a routerLink="/personal/pedido" class="btn secondary">Crear comanda</a></div>}</div>
  }@else if(page==='cobrar'){
    <div class="page-heading"><span class="eyebrow">CAJA</span><h1>Registrar cobro</h1><p>Resumen del importe de la mesa.</p></div><div class="glass form-panel"><label for="mesa-cobro">MESA</label><select id="mesa-cobro" [ngModel]="mesaId()" (ngModelChange)="mesaId.set(+$event)"><option *ngFor="let m of datos.mesas()" [value]="m.id">Mesa {{m.numero}}</option></select><div class="total-row"><span>Total de la mesa</span><strong>{{importeMesa()|currency:'MXN'}}</strong></div><label for="forma">FORMA DE PAGO</label><select id="forma" [(ngModel)]="forma"><option>Efectivo</option><option>Tarjeta</option><option>Transferencia</option></select><button class="btn primary" [disabled]="importeMesa()===0" (click)="cobrar()">Registrar cobro de prueba</button>@if(mensaje()){<p class="feedback" role="status">{{mensaje()}}</p>}</div>
  }@else if(page==='cierre'){
    <div class="page-heading"><span class="eyebrow">FINAL DEL TURNO</span><h1>Cierre de servicio</h1><p>Resumen de la actividad de esta sesión.</p></div><div class="glass form-panel"><h2>{{datos.comandas().length}} órdenes registradas</h2><p>Venta simulada del turno: {{ventaTotal()|currency:'MXN'}}</p><p>Estos datos son temporales y no constituyen un cierre de caja real.</p><a class="btn primary" routerLink="/personal/mesas">Volver a las mesas</a></div>
  }
`})
export class PersonalComponent {
  readonly page=inject(ActivatedRoute).snapshot.data['page'] as string;
  readonly datos=inject(RestauranteService);private router=inject(Router);
  readonly mesaId=signal(5);readonly cantidades=signal<Record<number,number>>({});readonly mensaje=signal('');
  readonly totalCantidad=computed(()=>Object.values(this.cantidades()).reduce((a,b)=>a+b,0));
  readonly total=computed(()=>this.datos.productos().reduce((a,p)=>a+p.precio*(this.cantidades()[p.id]||0),0));
  readonly importeMesa=computed(()=>this.datos.comandas().filter(o=>o.mesaId===this.mesaId()).reduce((a,o)=>a+o.total,0));
  readonly ventaTotal=computed(()=>this.datos.comandas().reduce((a,o)=>a+o.total,0));
  usuario='';pass='';forma='Efectivo';
  entrar():void{this.router.navigateByUrl('/personal/mesas');}
  seleccionarMesa(id:number):void{this.mesaId.set(id);this.router.navigateByUrl('/personal/pedido');}
  cantidad(id:number,d:number):void{this.cantidades.update(x=>({...x,[id]:Math.max(0,(x[id]||0)+d)}));}
  enviar():void{const items=Object.entries(this.cantidades()).filter(([,n])=>n>0).map(([id,n])=>({productoId:+id,cantidad:n}));if(!items.length)return;this.datos.crearComanda(this.mesaId(),items);this.cantidades.set({});this.router.navigateByUrl('/personal/cocina');}
  cobrar():void{this.mensaje.set('Cobro simulado. Se habilitará el registro real con el servidor.');}
}
