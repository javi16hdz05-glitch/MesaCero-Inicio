import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root', standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `<div class="layout"><aside class="sidebar"><div class="brand"><span class="brand-mark">MC</span><div><strong>MESA CERO</strong><small>Gestión gastronómica</small></div></div><nav aria-label="Navegación principal"><a routerLink="/" [routerLinkActiveOptions]="{exact:true}" routerLinkActive="active">Inicio</a><a routerLink="/mesas" routerLinkActive="active">Mesas</a><a routerLink="/carta" routerLinkActive="active">Carta</a><a routerLink="/comandas" routerLinkActive="active">Comandas</a></nav><div class="sidebar-foot">Prototipo académico · Angular 19<br>Modo demostración</div></aside><main class="main"><header class="topbar"><span>Restaurante / Panel administrativo</span><span class="status"><i></i> Datos de prueba</span></header><router-outlet /></main></div>`
})
export class AppComponent {}
