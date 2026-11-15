import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { TemaService } from './services/tema.service';
import { RestauranteService } from './services/restaurante.service';

@Component({
  selector:'app-root', standalone:true, imports:[RouterOutlet,RouterLink,RouterLinkActive],
  template:`<div class="app" [class.light]="tema.claro()" [class.admin-layout]="admin()" [class.staff-layout]="staff()">
    @if(admin()) {
      <aside class="admin-sidebar"><a class="admin-logo" routerLink="/admin/resumen">MESA <em>CERO</em><small>ADMIN CONSOLE</small></a><nav aria-label="Administración">
      @for(n of adminNav; track n.url){<a [routerLink]="n.url" routerLinkActive="active">{{n.label}}</a>}
      </nav><a class="admin-exit" routerLink="/bienvenida">Cerrar sesión</a></aside>
    }
    <div class="app-stage">
      @if(!admin()){
      <header class="brandbar"><a routerLink="/bienvenida" class="logo">MESA <em>CERO</em><small>{{staff()?'SERVICIO EN SALÓN':'COCINA DE AUTOR · OAXACA'}}</small></a><span class="brand-dot" aria-hidden="true"></span></header>
      }
      <div class="floating-actions"><button class="theme-toggle" (click)="tema.alternar()" [attr.aria-label]="tema.claro()?'Cambiar a modo oscuro':'Cambiar a modo claro'">{{tema.claro()?'☾ Oscuro':'☼ Claro'}}</button></div>
      @if(!admin()&&!staff()&&!hideNavigation()){
        <nav class="desktop-client-nav" aria-label="Navegación principal en escritorio">@for(n of clientNav;track n.url){<a [routerLink]="n.url" routerLinkActive="active"><img [src]="n.icon" alt="">{{n.label}}</a>}</nav>
      }
      <main [class.welcome-main]="url()==='/bienvenida'"><router-outlet /></main>
      @if(!admin() && !hideNavigation()){
        @if(staff()){
          <nav class="bottom-nav staff-nav" aria-label="Menú del mesero"><a routerLink="/personal/mesas" routerLinkActive="active">Mesas</a><a routerLink="/personal/pedido" routerLinkActive="active">Pedido</a><a routerLink="/personal/cobrar" routerLinkActive="active">Cobrar</a><a routerLink="/personal/cierre" routerLinkActive="active">Cerrar</a></nav>
        }@else{
          <nav class="bottom-nav client-nav" aria-label="Navegación del cliente">
          @for(n of clientNav;track n.url){<a [routerLink]="n.url" routerLinkActive="active"><img [src]="n.icon" alt=""><span>{{n.label}}</span>@if(n.label==='Carrito'&&datos.cantidadCarrito()>0){<b class="cart-badge">{{datos.cantidadCarrito()}}</b>}</a>}
          </nav>
        }
      }
    </div>
    <span class="demo-label">PROTOTIPO · DATOS DE DEMOSTRACIÓN</span>
  </div>`
})
export class AppComponent {
  readonly tema=inject(TemaService); readonly datos=inject(RestauranteService); private readonly router=inject(Router);
  readonly url=signal(this.router.url);
  readonly admin=computed(()=>this.url().startsWith('/admin'));
  readonly staff=computed(()=>this.url().startsWith('/personal'));
  readonly hideNavigation=computed(()=>['/bienvenida','/acceso','/registro','/recuperar','/identidad','/nueva-clave','/personal/acceso'].includes(this.url())||this.url().startsWith('/error/'));
  readonly clientNav=[{label:'Inicio',url:'/inicio',icon:'assets/home.svg'},{label:'Carta',url:'/carta',icon:'assets/menu.svg'},{label:'Reserva',url:'/reserva',icon:'assets/calendar.svg'},{label:'Carrito',url:'/carrito',icon:'assets/cart.svg'},{label:'Perfil',url:'/perfil',icon:'assets/user.svg'}];
  readonly adminNav=[{label:'Resumen',url:'/admin/resumen'},{label:'Mesas',url:'/admin/mesas'},{label:'Carta',url:'/admin/productos'},{label:'Reservas',url:'/admin/reservas'},{label:'Ventas',url:'/admin/ventas'}];
  constructor(){this.router.events.subscribe(e=>{if(e instanceof NavigationEnd){this.url.set(e.urlAfterRedirects.split('?')[0]);window.scrollTo({top:0,behavior:'instant'});}});}
}
