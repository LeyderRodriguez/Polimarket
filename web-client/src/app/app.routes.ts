import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { autenticado, invitado } from './core/auth/autenticado.guard';
import { inicioDe } from './core/auth/inicio';
import { conRol } from './core/auth/rol.guard';
import { SesionServicio } from './core/auth/sesion.servicio';
import { Shell } from './core/layout/shell/shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitado],
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Shell,
    canActivate: [autenticado],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => {
          const usuario = inject(SesionServicio).usuario();
          return usuario ? inicioDe(usuario.rol) : '/login';
        },
      },
      {
        path: 'disponibilidad',
        canActivate: [conRol('VENDEDOR')],
        loadComponent: () => import('./features/disponibilidad/disponibilidad').then((m) => m.Disponibilidad),
      },
      {
        path: 'ventas/nueva',
        canActivate: [conRol('VENDEDOR')],
        loadComponent: () => import('./features/ventas/nueva-venta/nueva-venta').then((m) => m.NuevaVenta),
      },
      {
        path: 'ventas',
        canActivate: [conRol('VENDEDOR')],
        loadComponent: () => import('./features/ventas/mis-ventas/mis-ventas').then((m) => m.MisVentas),
      },
      {
        path: 'clientes',
        canActivate: [conRol('VENDEDOR')],
        loadComponent: () => import('./features/clientes/clientes').then((m) => m.Clientes),
      },
      {
        path: 'entregas',
        canActivate: [conRol('ENTREGAS')],
        loadComponent: () => import('./features/entregas/entregas').then((m) => m.Entregas),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
