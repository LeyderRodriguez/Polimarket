import { Routes } from '@angular/router';
import { Ventas } from './components/ventas/ventas';
import { Disponibilidad } from './components/disponibilidad/disponibilidad';
import { Entregas } from './components/entregas/entregas';

export const routes: Routes = [
  { path: 'ventas', component: Ventas },
  { path: 'disponibilidad', component: Disponibilidad },
  { path: 'entregas', component: Entregas },
  { path: '', redirectTo: '/ventas', pathMatch: 'full' } // Ruta por defecto
];