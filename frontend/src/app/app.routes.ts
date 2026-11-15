import { Routes } from '@angular/router';
import { InicioComponent } from './pages/inicio.component';
import { MesasComponent } from './pages/mesas.component';
import { CartaComponent } from './pages/carta.component';
import { ComandasComponent } from './pages/comandas.component';

export const routes: Routes = [
  { path: '', component: InicioComponent, title: 'Inicio | Mesa Cero' },
  { path: 'mesas', component: MesasComponent, title: 'Mesas | Mesa Cero' },
  { path: 'carta', component: CartaComponent, title: 'Carta | Mesa Cero' },
  { path: 'comandas', component: ComandasComponent, title: 'Comandas | Mesa Cero' },
  { path: '**', redirectTo: '' }
];
