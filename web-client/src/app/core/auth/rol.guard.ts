import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import type { Rol } from '../api/tipos';
import { inicioDe } from './inicio';
import { SesionServicio } from './sesion.servicio';

/**
 * Evita mostrar pantallas que no son del rol. Es solo comodidad: quien decide el permiso real es el backend,
 * que responde 403 aunque alguien escriba la dirección a mano.
 */
export const conRol =
  (...roles: Rol[]): CanActivateFn =>
  () => {
    const router = inject(Router);
    const usuario = inject(SesionServicio).usuario();
    if (!usuario) {
      return router.createUrlTree(['/login']);
    }
    return roles.includes(usuario.rol) ? true : router.createUrlTree([inicioDe(usuario.rol)]);
  };
