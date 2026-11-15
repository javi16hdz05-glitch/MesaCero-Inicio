import { Routes } from '@angular/router';
import { ClienteComponent } from './pages/cliente.component';
import { PersonalComponent } from './pages/personal.component';
import { AdminComponent } from './pages/admin.component';
import { SistemaComponent } from './pages/sistema.component';

const cliente=(page:string)=>({component:ClienteComponent,data:{page}});
const personal=(page:string)=>({component:PersonalComponent,data:{page}});
const admin=(page:string)=>({component:AdminComponent,data:{page}});
export const routes:Routes=[
  {path:'',redirectTo:'bienvenida',pathMatch:'full'},
  {path:'bienvenida',...cliente('bienvenida')},{path:'inicio',...cliente('inicio')},{path:'carta',...cliente('carta')},
  {path:'bebidas',...cliente('bebidas')},{path:'detalle/:id',...cliente('detalle')},{path:'carrito',...cliente('carrito')},{path:'finalizar',...cliente('finalizar')},
  {path:'reserva',...cliente('reserva')},{path:'mapa',...cliente('mapa')},{path:'mesa/:id',...cliente('mesa')},
  {path:'confirmacion',...cliente('confirmacion')},{path:'acceso',...cliente('acceso')},{path:'registro',...cliente('registro')},
  {path:'perfil',...cliente('perfil')},{path:'reservas',...cliente('reservas')},{path:'pedidos',...cliente('pedidos')},
  {path:'personal/acceso',...personal('acceso')},{path:'personal/mesas',...personal('mesas')},{path:'personal/pedido',...personal('pedido')},
  {path:'personal/cocina',...personal('cocina')},{path:'personal/cobrar',...personal('cobrar')},{path:'personal/cierre',...personal('cierre')},
  {path:'admin/resumen',...admin('resumen')},{path:'admin/mesas',...admin('mesas')},{path:'admin/productos',...admin('productos')},
  {path:'admin/reservas',...admin('reservas')},{path:'admin/ventas',...admin('ventas')},
  {path:'recuperar',component:SistemaComponent,data:{page:'recuperar'}},{path:'identidad',component:SistemaComponent,data:{page:'identidad'}},
  {path:'nueva-clave',component:SistemaComponent,data:{page:'nueva-clave'}},{path:'error/:tipo',component:SistemaComponent,data:{page:'error'}},
  {path:'**',redirectTo:'bienvenida'}
];
