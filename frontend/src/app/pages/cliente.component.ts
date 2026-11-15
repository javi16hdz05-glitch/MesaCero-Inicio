import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RestauranteService } from '../services/restaurante.service';
import { Producto } from '../models/restaurant.models';

@Component({selector:'app-cliente',standalone:true,imports:[CurrencyPipe,FormsModule,RouterLink],template:`
  @if(page==='bienvenida') {
    <div class="welcome-grid"><div class="welcome-copy"><h1>Sabor que inspira.<br>Momentos que permanecen.</h1><p>Conoce una nueva forma de disfrutar la cocina oaxaqueña.</p></div>
      <div class="heritage-art" role="img" aria-label="Emblema decorativo de cocina tradicional"><div class="plate"><div class="plate-center"><span></span><i></i><b></b></div></div><span>TRADICIÓN · ELEGANCIA · INNOVACIÓN</span></div>
      <div class="welcome-cta"><h2>La mejor mesa empieza aquí.</h2><a class="btn primary" routerLink="/inicio">Explorar como invitado</a><a class="btn secondary" routerLink="/acceso">Iniciar sesión</a><a class="btn tiny" routerLink="/personal/acceso">Acceso para el personal →</a></div>
    </div>
  } @else if(page==='inicio') {
    <div class="discovery"><div class="search-wrap"><span>⌕</span><input aria-label="Buscar platillos y bebidas" placeholder="Buscar platillos y bebidas..." [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)"><a routerLink="/carta" aria-label="Abrir carta">☷</a></div>
      <div class="pills">@for(c of categoriasInicio;track c){<button [class.active]="categoria()===c" (click)="categoria.set(c)">{{c}}</button>}</div>
      <a class="explore-banner" routerLink="/carta"><span>▣ &nbsp; Explorar menú completo</span><b>→</b></a>
      <h2 class="section-title">Selección del chef</h2><p class="section-subtitle">Dos columnas · desliza y descubre</p>
      <div class="featured-grid">@for(p of productosFiltrados();track p.id){<article class="featured-card"><a [routerLink]="['/detalle',p.id]" class="featured-image"><img [src]="p.foto" [alt]="p.nombre">@if(p.etiqueta){<span class="ribbon">{{p.etiqueta}}</span>}</a><div class="featured-info"><h3>{{p.nombre}}</h3><div class="featured-actions"><span>{{p.precio|currency:'MXN':'symbol':'1.0-0'}} MXN</span><button [attr.aria-label]="'Agregar '+p.nombre" (click)="datos.cambiarCantidad(p.id,1)">+</button></div></div></article>}</div>
    </div>
  } @else if(page==='carta'||page==='bebidas') {
    <div class="page-heading"><span class="eyebrow">{{page==='bebidas'?'BARRA DE LA CASA':'MENÚ DE TEMPORADA'}}</span><h1>{{page==='bebidas'?'Bebidas y maridaje':'Nuestra carta'}}</h1><p>{{page==='bebidas'?'Una selección para acompañar cada momento.':'Sabores artesanales elaborados con ingredientes locales.'}}</p></div>
    @if(page==='carta') {<div class="pills menu-pills"><button [class.active]="categoria()==='Todos'" (click)="categoria.set('Todos')">Todos</button><button [class.active]="categoria()==='Oaxaqueños'" (click)="categoria.set('Oaxaqueños')">Oaxaqueños</button><a class="pill-link" routerLink="/bebidas">Bebidas</a></div>}
    @if(page==='bebidas'){<a class="btn secondary compact" routerLink="/carta">← Ver platillos de la carta</a>}
    <div class="menu-list">@for(p of cartaFiltrada();track p.id){<a class="menu-item glass" [routerLink]="['/detalle',p.id]"><img [src]="p.foto" [alt]="p.nombre"><div><h3>{{p.nombre}}</h3><p>{{p.descripcion}}</p><strong>{{p.precio|currency:'MXN':'symbol':'1.0-0'}} MXN</strong></div></a>}</div>
    <a class="btn secondary cart-action" routerLink="/carrito">Ir a mi pedido @if(datos.cantidadCarrito()>0){({{datos.cantidadCarrito()}})}</a>
  } @else if(page==='detalle') {
    @if(producto();as p){<div class="page-heading"><span class="eyebrow">SELECCIÓN DEL CHEF</span><h1>{{p.nombre}}</h1></div><div class="detail-layout"><img class="detail-photo" [src]="p.foto" [alt]="p.nombre"><div class="detail-copy"><h2>{{p.nombre}}</h2><p>{{p.descripcion}}</p><strong class="gold-price">{{p.precio|currency:'MXN':'symbol':'1.0-0'}} MXN</strong><div class="quantity"><button (click)="datos.cambiarCantidad(p.id,-1)" [disabled]="datos.cantidad(p.id)===0">−</button><span>{{datos.cantidad(p.id)}}</span><button (click)="datos.cambiarCantidad(p.id,1)">+</button></div><button class="btn primary" (click)="datos.cambiarCantidad(p.id,1); aviso.set('Producto agregado a tu pedido')">Agregar al pedido</button><a class="btn secondary" routerLink="/carrito">Ver mi carrito</a>@if(aviso()){<p class="feedback" role="status">{{aviso()}}</p>}</div></div>}
  } @else if(page==='carrito') {
    <div class="page-heading"><span class="eyebrow">CASI LISTO</span><h1>Tu pedido</h1><p>Revisa los productos que has elegido.</p></div>
    <div class="checkout-layout"><div class="menu-list">@for(item of datos.carrito();track item.productoId){@if(encontrar(item.productoId);as p){<div class="menu-item glass"><img [src]="p.foto" [alt]="p.nombre"><div><h3>{{p.nombre}}</h3><p>{{item.cantidad}} {{item.cantidad===1?'unidad':'unidades'}}</p><strong>{{p.precio*item.cantidad|currency:'MXN':'symbol':'1.0-0'}} MXN</strong><div class="inline-qty"><button (click)="datos.cambiarCantidad(p.id,-1)">−</button><span>{{item.cantidad}}</span><button (click)="datos.cambiarCantidad(p.id,1)">+</button></div></div></div>}}@empty{<div class="glass empty-state"><h2>Tu carrito está vacío</h2><p>Explora nuestra carta y elige lo que más te guste.</p><a class="btn secondary" routerLink="/carta">Explorar carta</a></div>}</div>
      <aside class="glass total-panel"><div><span>Subtotal</span><strong>{{datos.totalCarrito()|currency:'MXN':'symbol':'1.0-0'}}</strong></div><div class="grand-total"><b>Total</b><strong>{{datos.totalCarrito()|currency:'MXN':'symbol':'1.0-0'}}</strong></div></aside></div>
    <button class="btn primary bottom-action" [disabled]="!datos.cantidadCarrito()" (click)="continuarPedido()">Continuar con el pedido</button>
  } @else if(page==='finalizar') {
    <div class="page-heading"><span class="eyebrow">CONFIRMACIÓN</span><h1>Confirma tu pedido</h1><p>Elige la mesa donde deseas recibir tu pedido de demostración.</p></div><div class="glass form-panel"><label for="mesa-orden">MESA DE DESTINO</label><select id="mesa-orden" [ngModel]="datos.mesaElegida()||''" (ngModelChange)="datos.mesaElegida.set($event?+$event:null)"><option value="">Selecciona una mesa</option>@for(m of datos.mesas();track m.id){<option [value]="m.id">Mesa M{{m.numero}} · {{m.capacidad}} personas</option>}</select><p class="form-note">Subtotal: {{datos.totalCarrito()|currency:'MXN'}}. Esta operación no generará un cobro real.</p><button class="btn primary" [disabled]="!datos.mesaElegida()" (click)="finalizarPedido()">Registrar pedido de prueba</button><a class="btn secondary" routerLink="/carrito">Volver a mi carrito</a></div>
  } @else if(page==='reserva') {
    <div class="page-heading"><span class="eyebrow">RESERVACIONES</span><h1>Reserva tu mesa</h1><p>Escoge la fecha, la hora y el número de invitados.</p></div>
    <form class="glass form-panel" (ngSubmit)="consultarMesa()"><label for="fecha">FECHA DE VISITA</label><input id="fecha" type="date" required [min]="hoy" [ngModel]="datos.reservaPendiente().fecha" (ngModelChange)="actualizarReserva('fecha',$event)" name="fecha">
      <label for="hora">HORA DE LLEGADA</label><input id="hora" type="time" required [ngModel]="datos.reservaPendiente().hora" (ngModelChange)="actualizarReserva('hora',$event)" name="hora">
      <label>NÚMERO DE PERSONAS</label><div class="guest-counter"><button type="button" (click)="personas(-1)" aria-label="Reducir invitados">−</button><span>{{datos.reservaPendiente().personas}} invitados</span><button type="button" (click)="personas(1)" aria-label="Aumentar invitados">+</button></div>
      <label for="zona">ZONA PREFERIDA</label><select id="zona" name="zona" [ngModel]="datos.reservaPendiente().zona" (ngModelChange)="actualizarReserva('zona',$event)"><option>Salón interior / Terraza</option><option>Salón interior</option><option>Terraza</option></select><p class="form-note">Selecciona una mesa disponible en el siguiente paso.</p><button type="submit" class="btn primary">Consultar disponibilidad</button>
    </form>
  } @else if(page==='mapa') {
    <div class="page-heading"><span class="eyebrow">DISPONIBILIDAD</span><h1>Elige tu mesa</h1><p>Verde: disponible · Vino: ocupada.</p></div>
    <div class="glass map-panel"><span class="entrance">ENTRADA</span><div class="map-grid">@for(m of datos.mesas();track m.id){<button class="map-table" [class.busy]="m.estado!=='disponible'" [class.selected]="datos.mesaElegida()===m.id" [disabled]="m.estado!=='disponible'" (click)="datos.mesaElegida.set(m.id)"><b>M{{m.numero}}</b><small>{{m.estado==='disponible'?'Libre':'Ocup.'}}</small></button>}</div><div class="legend"><span>● Disponible</span><span>● Ocupada</span><span>● Seleccionada</span></div></div>
    <div class="glass selected-summary"><strong>{{datos.reservaPendiente().fecha||'Fecha pendiente'}} · {{datos.reservaPendiente().hora}} · {{datos.reservaPendiente().personas}} PERSONAS</strong><p>{{datos.mesaElegida()?'Mesa M'+datos.mesaElegida()+' seleccionada.':'Toca una mesa disponible para continuar.'}}</p></div>
    <button class="btn primary bottom-action" [disabled]="!datos.mesaElegida()||!datos.reservaPendiente().fecha" (click)="router.navigate(['/mesa',datos.mesaElegida()])">Continuar con mesa elegida</button><a class="btn secondary bottom-action" routerLink="/reserva">Cambiar fecha</a>
  } @else if(page==='mesa') {
    <div class="page-heading"><span class="eyebrow">TU ELECCIÓN</span><h1>Mesa M{{datos.mesaElegida()}}</h1><p>Revisa tu selección antes de reservar.</p></div><div class="glass form-panel"><div class="selected-summary"><strong>{{datos.reservaPendiente().fecha}} · {{datos.reservaPendiente().hora}}</strong><p>{{datos.reservaPendiente().personas}} invitados · {{datos.reservaPendiente().zona}}</p></div><button class="btn primary" (click)="confirmarReserva()">Confirmar reservación</button><a routerLink="/mapa" class="btn secondary">Elegir otra mesa</a></div>
  } @else if(page==='confirmacion') {
    <div class="page-heading"><span class="eyebrow">RESERVACIÓN CONFIRMADA</span><h1>¡Te esperamos!</h1><p>Tu selección se guardó en esta demostración.</p></div><div class="glass form-panel celebration"><span class="success-orb">✓</span><h2>Gracias por elegir Mesa Cero</h2><p>Consulta tus reservas durante esta sesión.</p><a class="btn primary" routerLink="/reservas">Ver mis reservas</a><a class="btn secondary" routerLink="/inicio">Volver al inicio</a></div>
  } @else if(page==='acceso'||page==='registro') {
    <div class="page-heading"><span class="eyebrow">IDENTIFICACIÓN</span><h1>{{page==='acceso'?'Accede a tu cuenta':'Crea tu cuenta'}}</h1><p>Solo te pedimos acceso al reservar o confirmar tu pedido.</p></div>
    <form class="glass form-panel" (ngSubmit)="accesoDemo()">@if(page==='registro'){<label for="nombre">NOMBRE COMPLETO</label><input id="nombre" name="nombre" required [(ngModel)]="nombre" placeholder="Tu nombre">}<label for="telefono">TELÉFONO</label><input id="telefono" name="telefono" type="tel" required [(ngModel)]="telefono" placeholder="+52 951 000 0000"><label for="password">CONTRASEÑA</label><input id="password" name="password" type="password" minlength="6" required [(ngModel)]="password" placeholder="••••••••••••">@if(page==='acceso'){<a routerLink="/recuperar" class="recover-link">¿Olvidaste tu contraseña?</a>}<button class="btn primary" type="submit">{{page==='acceso'?'Entrar a mi cuenta':'Crear cuenta'}}</button><a class="btn secondary" [routerLink]="page==='acceso'?'/registro':'/acceso'">{{page==='acceso'?'Crear cuenta':'Ya tengo cuenta'}}</a>@if(aviso()){<p class="feedback" role="status">{{aviso()}}</p>}</form><p class="section-subtitle extra-space">También puedes continuar sin iniciar sesión.</p><a class="btn secondary bottom-action" routerLink="/inicio">Volver como invitado</a>
  } @else if(page==='perfil') {
    <div class="page-heading"><span class="eyebrow">TU EXPERIENCIA</span><h1>Mi perfil</h1><p>Todo lo que necesitas en un solo lugar.</p></div><div class="glass profile-panel"><div class="avatar">MC</div><h2>Invitado Mesa Cero</h2><p>Experiencia de demostración</p><a class="profile-link" routerLink="/reservas">Mis reservaciones <span>→</span></a><a class="profile-link" routerLink="/pedidos">Mis pedidos <span>→</span></a><a class="profile-link" routerLink="/acceso">Acceder a mi cuenta <span>→</span></a></div>
  } @else if(page==='reservas') {
    <div class="page-heading"><span class="eyebrow">TU HISTORIAL</span><h1>Mis reservas</h1><p>Reservaciones guardadas en esta sesión.</p></div><div class="menu-list">@for(r of datos.reservas();track r.id){<div class="glass simple-record"><h3>Mesa M{{r.mesaId}}</h3><span>{{r.fecha}} · {{r.hora}}</span><p>{{r.personas}} personas · {{r.zona}}</p></div>}@empty{<div class="glass empty-state"><h2>No tienes reservaciones</h2><p>Reserva una mesa y vuelve aquí para consultarla.</p><a class="btn primary" routerLink="/reserva">Reservar ahora</a></div>}</div>
  } @else if(page==='pedidos') {
    <div class="page-heading"><span class="eyebrow">TU HISTORIAL</span><h1>Mis pedidos</h1><p>Órdenes de demostración durante esta sesión.</p></div><div class="menu-list">@for(o of datos.comandas();track o.id){<div class="glass simple-record"><h3>Pedido #{{o.id.toString().slice(-5)}}</h3><span>Mesa M{{o.mesaId}} · {{o.total|currency:'MXN'}}</span><p>Estado: {{o.estado}}</p></div>}@empty{<div class="glass empty-state"><h2>Sin pedidos todavía</h2><a class="btn primary" routerLink="/carta">Explorar nuestra carta</a></div>}</div>
  }
`})
export class ClienteComponent {
  private route=inject(ActivatedRoute);readonly router=inject(Router);readonly datos=inject(RestauranteService);
  readonly page=this.route.snapshot.data['page'] as string;
  readonly hoy=new Date().toISOString().slice(0,10);
  readonly categoria=signal('Todos');readonly busqueda=signal('');readonly aviso=signal('');
  readonly categoriasInicio=['Todos','Oaxaqueños','Fuertes','Bebidas'];
  readonly producto=computed(()=>this.datos.productos().find(p=>p.id===Number(this.route.snapshot.paramMap.get('id'))));
  readonly productosFiltrados=computed(()=>this.datos.productos().filter(p=>p.disponible&&(this.categoria()==='Todos'||p.categoria===this.categoria())&&p.nombre.toLowerCase().includes(this.busqueda().toLowerCase())));
  readonly cartaFiltrada=computed(()=>this.datos.productos().filter(p=>p.disponible&&(this.page==='bebidas'?p.categoria==='Bebidas':this.categoria()==='Todos'?p.categoria!=='Bebidas':p.categoria===this.categoria())));
  nombre='';telefono='';password='';
  encontrar(id:number):Producto|undefined{return this.datos.productos().find(p=>p.id===id);}
  actualizarReserva(key:'fecha'|'hora'|'zona',value:string):void{this.datos.reservaPendiente.update(x=>({...x,[key]:value}));}
  personas(delta:number):void{this.datos.reservaPendiente.update(x=>({...x,personas:Math.min(12,Math.max(1,x.personas+delta))}));}
  consultarMesa():void{this.datos.mesaElegida.set(null);this.router.navigateByUrl('/mapa');}
  confirmarReserva():void{try{this.datos.guardarReserva();this.router.navigateByUrl('/confirmacion');}catch{this.aviso.set('Selecciona una fecha y una mesa disponible.');}}
  continuarPedido():void{if(this.datos.cantidadCarrito())this.router.navigateByUrl('/finalizar');}
  finalizarPedido():void{if(!this.datos.mesaElegida())return;this.datos.confirmarPedido();this.router.navigateByUrl('/pedidos');}
  accesoDemo():void{this.aviso.set('El acceso real se habilitará al integrar el backend. Puedes continuar como invitado.');}
}
