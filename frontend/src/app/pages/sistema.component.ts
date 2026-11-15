import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

const estados:Record<string,{label:string;titulo:string;mensaje:string;boton:string;ruta:string}>={
  '404':{label:'ERROR 404 · NAVEGACIÓN',titulo:'Esta página no está aquí',mensaje:'Parece que la dirección que buscas ya no existe. Puedes volver y seguir explorando MesaCero.',boton:'Volver al inicio',ruta:'/inicio'},
  '500':{label:'ERROR 500 · SERVICIO',titulo:'Algo no salió como esperábamos',mensaje:'Estamos teniendo un inconveniente. Inténtalo nuevamente en unos momentos.',boton:'Volver al inicio',ruta:'/inicio'},
  'sin-conexion':{label:'CONEXIÓN INTERRUMPIDA',titulo:'Perdimos la conexión',mensaje:'Revisa tu conexión a internet y vuelve a intentarlo.',boton:'Volver al inicio',ruta:'/inicio'},
  'mesa-no-disponible':{label:'RESERVACIONES',titulo:'Esta mesa ya no está disponible',mensaje:'Alguien más eligió esta mesa. Puedes consultar otra opción.',boton:'Elegir otra mesa',ruta:'/mapa'},
  'producto-agotado':{label:'CARTA',titulo:'Este platillo se ha agotado',mensaje:'Explora otros sabores de nuestra carta.',boton:'Volver a la carta',ruta:'/carta'},
  'pedido-no-confirmado':{label:'PEDIDOS',titulo:'No pudimos confirmar tu pedido',mensaje:'Conserva tu selección e inténtalo nuevamente.',boton:'Ver mi pedido',ruta:'/carrito'},
  'cobro-no-confirmado':{label:'PAGOS',titulo:'No pudimos confirmar el cobro',mensaje:'Consulta al personal antes de intentar nuevamente.',boton:'Volver al salón',ruta:'/personal/mesas'},
  'sesion-expirada':{label:'ERROR 401 · SESIÓN',titulo:'Tu sesión ha terminado',mensaje:'Por seguridad, inicia sesión nuevamente.',boton:'Iniciar sesión',ruta:'/acceso'},
  'acceso-denegado':{label:'ERROR 403 · ACCESO',titulo:'No tienes acceso a esta sección',mensaje:'Comprueba tu cuenta o vuelve a la página principal.',boton:'Volver al inicio',ruta:'/inicio'},
  'sin-reservas':{label:'RESERVACIONES',titulo:'Aún no tienes reservaciones',mensaje:'Explora nuestras mesas y reserva tu lugar.',boton:'Reservar ahora',ruta:'/reserva'},
  'credenciales':{label:'IDENTIFICACIÓN',titulo:'Revisa tus datos de acceso',mensaje:'No pudimos verificar las credenciales. Puedes intentarlo otra vez.',boton:'Volver a iniciar sesión',ruta:'/acceso'},
  'campos':{label:'FORMULARIO',titulo:'Faltan datos obligatorios',mensaje:'Comprueba los campos señalados y vuelve a intentarlo.',boton:'Volver al formulario',ruta:'/registro'}
};

@Component({selector:'app-sistema',standalone:true,imports:[FormsModule,RouterLink],template:`
  @if(page==='error'){
    <div class="glass error-card"><span class="eyebrow">{{estado.label}}</span><h1>{{estado.titulo}}</h1><p>{{estado.mensaje}}</p><a class="btn primary" [routerLink]="estado.ruta">{{estado.boton}}</a></div>
  }@else{
    <div class="page-heading"><span class="eyebrow">RECUPERAR ACCESO</span><h1>{{page==='recuperar'?'¿Olvidaste tu contraseña?':page==='identidad'?'Comprueba tu identidad':'Protege tu cuenta'}}</h1><p>{{page==='recuperar'?'Ingresa tu número para continuar.':page==='identidad'?'Ingresa el código de verificación.':'Crea una nueva contraseña segura.'}}</p></div>
    <form class="glass form-panel recovery-card" (ngSubmit)="siguiente()"><label for="dato">{{page==='recuperar'?'TELÉFONO':page==='identidad'?'CÓDIGO DE VERIFICACIÓN':'NUEVA CONTRASEÑA'}}</label><input id="dato" [type]="page==='nueva-clave'?'password':'text'" name="dato" [(ngModel)]="dato" [placeholder]="page==='recuperar'?'+52 951 000 0000':page==='identidad'?'000000':'••••••••'" required><p class="form-note">{{page==='recuperar'?'Usaremos este número para verificar tu identidad.':'Flujo visual de demostración; no se envían códigos ni se modifica ninguna cuenta real.'}}</p><button type="submit" class="btn primary">{{page==='recuperar'?'Enviar código':page==='identidad'?'Verificar código':'Cambiar contraseña'}}</button>@if(aviso()){<p class="feedback" role="status">{{aviso()}}</p>}</form><a class="btn secondary bottom-action" routerLink="/acceso">Volver</a>
  }
`})
export class SistemaComponent{
  private ruta=inject(ActivatedRoute);private router=inject(Router);
  readonly page=this.ruta.snapshot.data['page'] as string;
  readonly estado=estados[this.ruta.snapshot.paramMap.get('tipo')??'404']??estados['404'];
  readonly aviso=signal('');dato='';
  siguiente():void{
    if(this.page==='recuperar')this.router.navigateByUrl('/identidad');
    else if(this.page==='identidad')this.router.navigateByUrl('/nueva-clave');
    else this.aviso.set('Demostración: el cambio real de contraseña se habilitará con el backend.');
  }
}
